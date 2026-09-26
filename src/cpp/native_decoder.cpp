#include <stddef.h>
#include <stdio.h>
#include <stdint.h>

#include <jni.h>
#include <cstring>
#include <cstdlib>
#include <vector>
#include <algorithm>

extern "C" {
    #include "pl_mpeg.h"
    #include "minimp4.h"
}

#include "codec_api.h"
#include "stb_image.h"

#define COBE_H264_DEBUG 0
#if COBE_H264_DEBUG
    #define H264_LOG(...) fprintf(stderr, __VA_ARGS__)
#else
    #define H264_LOG(...) do {} while (0)
#endif

enum class VideoFormat { UNKNOWN, MPEG1_PS, MP4_H264, GIF };

struct DecoderState {
    VideoFormat format = VideoFormat::UNKNOWN;
    int width = 0, height = 0, frame_count = 0;
    double fps = 30.0;

    uint8_t* rgba_buffer = nullptr;
    size_t   rgba_buffer_size = 0;
    std::vector<uint8_t> file_data;

    int current_sample = 0;

    plm_t* plm = nullptr;

    MP4D_demux_t mp4 = {};
    int h264_track = -1;
    ISVCDecoder* h264_dec = nullptr;
    std::vector<uint8_t> sps_annexb;
    std::vector<uint8_t> pps_annexb;
    bool h264_has_sps_pps = false;

    uint8_t* gif_pixels = nullptr;
    int* gif_delays = nullptr;
};

static VideoFormat detect_format(const uint8_t* data, size_t size) {
    if (size < 16) return VideoFormat::UNKNOWN;
    if (size >= 8 && std::memcmp(data + 4, "ftyp", 4) == 0) return VideoFormat::MP4_H264;
    if (data[0] == 0 && data[1] == 0 && data[2] == 1 && data[3] == 0xBA) return VideoFormat::MPEG1_PS;
    if (std::memcmp(data, "GIF8", 4) == 0) return VideoFormat::GIF;
    return VideoFormat::UNKNOWN;
}

static int mp4_read_callback(int64_t offset, void* buffer, size_t size, void* token) {
    auto* data = reinterpret_cast<std::vector<uint8_t>*>(token);
    if (offset < 0) return 1;
    size_t off = (size_t)offset;
    if (off + size > data->size()) return 1;
    std::memcpy(buffer, data->data() + off, size);
    return 0;
}

static void yuv420_to_rgba(const uint8_t* y_plane, int y_stride,
                           const uint8_t* u_plane, int u_stride,
                           const uint8_t* v_plane, int v_stride,
                           int w, int h, uint8_t* rgba)
{
    for (int y = 0; y < h; ++y) {
        const uint8_t* yrow = y_plane + (size_t)y * y_stride;
        const uint8_t* urow = u_plane + (size_t)(y / 2) * u_stride;
        const uint8_t* vrow = v_plane + (size_t)(y / 2) * v_stride;
        uint8_t* outrow = rgba + (size_t)y * w * 4;

        for (int x = 0; x < w; ++x) {
            int Y = yrow[x];
            int U = urow[x / 2];
            int V = vrow[x / 2];

            int C = Y - 16;
            int D = U - 128;
            int E = V - 128;

            int R = (298 * C + 409 * E + 128) >> 8;
            int G = (298 * C - 100 * D - 208 * E + 128) >> 8;
            int B = (298 * C + 516 * D + 128) >> 8;

            outrow[x*4+0] = (uint8_t)std::clamp(R, 0, 255);
            outrow[x*4+1] = (uint8_t)std::clamp(G, 0, 255);
            outrow[x*4+2] = (uint8_t)std::clamp(B, 0, 255);
            outrow[x*4+3] = 255;
        }
    }
}

static bool init_mpeg1(DecoderState* s) {
    s->plm = plm_create_with_memory(s->file_data.data(), s->file_data.size(), 0);
    if (!s->plm) return false;
    plm_set_audio_enabled(s->plm, 0);
    s->width  = plm_get_width(s->plm);
    s->height = plm_get_height(s->plm);
    s->fps    = plm_get_framerate(s->plm);
    s->frame_count = -1;
    return true;
}

static bool init_gif(DecoderState* s) {
    int x = 0, y = 0, z = 0, comp = 0;
    int* delays = nullptr;
    uint8_t* pixels = stbi_load_gif_from_memory(
        s->file_data.data(), (int)s->file_data.size(),
        &delays, &x, &y, &z, &comp, 4);
    if (!pixels || z <= 0) return false;

    s->gif_pixels = pixels;
    s->gif_delays = delays;
    s->width      = x;
    s->height     = y;
    s->frame_count = z;

    if (delays) {
        long total = 0;
        for (int i = 0; i < z; ++i) total += delays[i];
        double avg = (double)total / z;
        s->fps = avg > 0 ? 1000.0 / avg : 30.0;
    }
    return true;
}

static void build_annexb(const uint8_t* sample, uint32_t sample_size,
                         const std::vector<uint8_t>* sps,
                         const std::vector<uint8_t>* pps,
                         std::vector<uint8_t>& out)
{
    out.clear();
    out.reserve(sample_size + 64);

    if (sps && !sps->empty()) out.insert(out.end(), sps->begin(), sps->end());
    if (pps && !pps->empty()) out.insert(out.end(), pps->begin(), pps->end());

    const uint8_t* p = sample;
    uint32_t remaining = sample_size;
    while (remaining >= 4) {
        uint32_t nal_len = ((uint32_t)p[0] << 24) | ((uint32_t)p[1] << 16) |
                           ((uint32_t)p[2] <<  8) |  (uint32_t)p[3];
        p += 4;
        remaining -= 4;
        if (nal_len == 0 || nal_len > remaining) break;

        out.push_back(0); out.push_back(0);
        out.push_back(0); out.push_back(1);
        out.insert(out.end(), p, p + nal_len);

        p += nal_len;
        remaining -= nal_len;
    }
}

static bool init_mp4_h264(DecoderState* s) {
    if (!MP4D_open(&s->mp4, mp4_read_callback, &s->file_data,
                   (int64_t)s->file_data.size())) {
        H264_LOG("[h264] MP4D_open FAILED\n");
        return false;
    }
    H264_LOG("[h264] MP4D_open ok, track_count=%u\n", s->mp4.track_count);

    for (unsigned i = 0; i < s->mp4.track_count; ++i) {
        MP4D_track_t* t = &s->mp4.track[i];
        H264_LOG("[h264]   track[%u] handler=0x%08x samples=%u\n",
                 i, t->handler_type, t->sample_count);
        if (t->handler_type == MP4D_HANDLER_TYPE_VIDE && s->h264_track < 0)
            s->h264_track = (int)i;
    }
    if (s->h264_track < 0) {
        H264_LOG("[h264] no video track\n");
        return false;
    }
    H264_LOG("[h264] using video track %d\n", s->h264_track);

    MP4D_track_t* track = &s->mp4.track[s->h264_track];
    s->width  = track->SampleDescription.video.width;
    s->height = track->SampleDescription.video.height;

    if (track->sample_count > 0 && track->timescale > 0) {
        uint64_t dur = ((uint64_t)track->duration_hi << 32) | track->duration_lo;
        if (dur > 0) {
            double total_sec = (double)dur / (double)track->timescale;
            if (total_sec > 0.0) s->fps = (double)track->sample_count / total_sec;
        }
    }
    s->frame_count = (int)track->sample_count;
    H264_LOG("[h264] track meta: %dx%d fps=%.2f frames=%d\n",
             s->width, s->height, s->fps, s->frame_count);

    if (WelsCreateDecoder(&s->h264_dec) != 0 || !s->h264_dec) {
        H264_LOG("[h264] WelsCreateDecoder FAILED\n");
        return false;
    }

    int logLevel = WELS_LOG_QUIET;
    s->h264_dec->SetOption(DECODER_OPTION_TRACE_LEVEL, &logLevel);

    SDecodingParam param = {};
    param.sVideoProperty.eVideoBsType = VIDEO_BITSTREAM_AVC;
    param.eEcActiveIdc = ERROR_CON_SLICE_COPY;
    param.uiTargetDqLayer = (uint8_t)-1;

    if (s->h264_dec->Initialize(&param) != 0) {
        H264_LOG("[h264] OpenH264 Initialize FAILED\n");
        WelsDestroyDecoder(s->h264_dec);
        s->h264_dec = nullptr;
        return false;
    }

    for (int nsps = 0; nsps <= 1; ++nsps) {
        int sps_size = 0, pps_size = 0;
        const void* sps = MP4D_read_sps(&s->mp4, (unsigned)s->h264_track,
                                         nsps, &sps_size);
        const void* pps = MP4D_read_pps(&s->mp4, (unsigned)s->h264_track,
                                         nsps, &pps_size);
        H264_LOG("[h264] nsps=%d: sps=%p size=%d, pps=%p size=%d\n",
                 nsps, sps, sps_size, pps, pps_size);

        if (sps && sps_size > 0 && pps && pps_size > 0) {
            s->sps_annexb = {0, 0, 0, 1};
            s->sps_annexb.insert(s->sps_annexb.end(),
                                 (const uint8_t*)sps,
                                 (const uint8_t*)sps + sps_size);
            s->pps_annexb = {0, 0, 0, 1};
            s->pps_annexb.insert(s->pps_annexb.end(),
                                 (const uint8_t*)pps,
                                 (const uint8_t*)pps + pps_size);
            s->h264_has_sps_pps = true;
            H264_LOG("[h264] SPS[%d] PPS[%d]\n", sps_size, pps_size);
            break;
        }
    }
    return true;
}

extern "C" JNIEXPORT jlong JNICALL
Java_v_akfz_cobe_nat_NativeVideoDecoder_open(
        JNIEnv* env, jclass, jbyteArray jdata)
{
    jsize len = env->GetArrayLength(jdata);
    if (len <= 0) return 0;

    auto* s = new DecoderState();
    s->file_data.resize(len);
    env->GetByteArrayRegion(jdata, 0, len,
                            reinterpret_cast<jbyte*>(s->file_data.data()));

    s->format = detect_format(s->file_data.data(), s->file_data.size());

    bool ok = false;
    switch (s->format) {
        case VideoFormat::MPEG1_PS: ok = init_mpeg1(s); break;
        case VideoFormat::MP4_H264: ok = init_mp4_h264(s); break;
        case VideoFormat::GIF:      ok = init_gif(s); break;
        default: break;
    }

    if (!ok) {
        if (s->plm)      plm_destroy(s->plm);
        if (s->h264_dec) WelsDestroyDecoder(s->h264_dec);
        if (s->h264_track >= 0) MP4D_close(&s->mp4);
        if (s->gif_pixels) stbi_image_free(s->gif_pixels);
        if (s->gif_delays) stbi_image_free(s->gif_delays);
        delete s;
        return 0;
    }

    s->rgba_buffer_size = (size_t)s->width * s->height * 4;
    s->rgba_buffer = (uint8_t*)std::malloc(s->rgba_buffer_size);
    if (!s->rgba_buffer) {
        if (s->plm)      plm_destroy(s->plm);
        if (s->h264_dec) WelsDestroyDecoder(s->h264_dec);
        if (s->h264_track >= 0) MP4D_close(&s->mp4);
        if (s->gif_pixels) stbi_image_free(s->gif_pixels);
        if (s->gif_delays) stbi_image_free(s->gif_delays);
        delete s;
        return 0;
    }

    return reinterpret_cast<jlong>(s);
}

extern "C" JNIEXPORT jint JNICALL
Java_v_akfz_cobe_nat_NativeVideoDecoder_getWidth(
        JNIEnv*, jclass, jlong h) {
    auto* s = reinterpret_cast<DecoderState*>(h);
    return s ? s->width : 0;
}

extern "C" JNIEXPORT jint JNICALL
Java_v_akfz_cobe_nat_NativeVideoDecoder_getHeight(
        JNIEnv*, jclass, jlong h) {
    auto* s = reinterpret_cast<DecoderState*>(h);
    return s ? s->height : 0;
}

extern "C" JNIEXPORT jdouble JNICALL
Java_v_akfz_cobe_nat_NativeVideoDecoder_getFps(
        JNIEnv*, jclass, jlong h) {
    auto* s = reinterpret_cast<DecoderState*>(h);
    return s ? s->fps : 30.0;
}

extern "C" JNIEXPORT jint JNICALL
Java_v_akfz_cobe_nat_NativeVideoDecoder_getFrameCount(
        JNIEnv*, jclass, jlong h) {
    auto* s = reinterpret_cast<DecoderState*>(h);
    return s ? s->frame_count : 0;
}

static bool reset_h264_decoder(DecoderState* s) {
    if (s->h264_dec) {
        WelsDestroyDecoder(s->h264_dec);
        s->h264_dec = nullptr;
    }
    if (WelsCreateDecoder(&s->h264_dec) != 0 || !s->h264_dec) return false;

    int logLevel = WELS_LOG_QUIET;
    s->h264_dec->SetOption(DECODER_OPTION_TRACE_LEVEL, &logLevel);

    SDecodingParam param = {};
    param.sVideoProperty.eVideoBsType = VIDEO_BITSTREAM_AVC;
    param.eEcActiveIdc = ERROR_CON_SLICE_COPY;
    param.uiTargetDqLayer = (uint8_t)-1;

    if (s->h264_dec->Initialize(&param) != 0) {
        WelsDestroyDecoder(s->h264_dec);
        s->h264_dec = nullptr;
        return false;
    }
    return true;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_v_akfz_cobe_nat_NativeVideoDecoder_decodeFrame(
        JNIEnv* env, jclass, jlong handle, jint index, jobject out_buffer)
{
    auto* s = reinterpret_cast<DecoderState*>(handle);
    if (!s || !s->rgba_buffer) return JNI_FALSE;

    void* dst = env->GetDirectBufferAddress(out_buffer);
    if (!dst) return JNI_FALSE;

    switch (s->format) {

        case VideoFormat::MPEG1_PS: {
            if (index < s->current_sample) {
                plm_destroy(s->plm);
                s->plm = plm_create_with_memory(
                    s->file_data.data(), s->file_data.size(), 0);
                plm_set_audio_enabled(s->plm, 0);
                s->current_sample = 0;
            }
            while (s->current_sample < index) {
                if (!plm_decode_video(s->plm)) return JNI_FALSE;
                s->current_sample++;
            }
            plm_frame_t* frame = plm_decode_video(s->plm);
            if (!frame) return JNI_FALSE;
            plm_frame_to_rgba(frame, s->rgba_buffer, s->width * 4);
            std::memcpy(dst, s->rgba_buffer, s->rgba_buffer_size);
            s->current_sample++;
            return JNI_TRUE;
        }

        case VideoFormat::MP4_H264: {
            if (index < s->current_sample) {
                if (!reset_h264_decoder(s)) return JNI_FALSE;
                s->current_sample = 0;
            }

            thread_local std::vector<uint8_t> annexb;

            while (s->current_sample < s->frame_count) {
                unsigned frame_bytes = 0, timestamp = 0, duration = 0;
                auto offset = MP4D_frame_offset(
                    &s->mp4, (unsigned)s->h264_track,
                    (unsigned)s->current_sample,
                    &frame_bytes, &timestamp, &duration);

                if (frame_bytes == 0) {
                    H264_LOG("[h264] frame %d: frame_bytes==0, stop\n",
                             s->current_sample);
                    return JNI_FALSE;
                }

                const std::vector<uint8_t>* sps_arg =
                    (s->current_sample == 0) ? &s->sps_annexb : nullptr;
                const std::vector<uint8_t>* pps_arg =
                    (s->current_sample == 0) ? &s->pps_annexb : nullptr;

                build_annexb(s->file_data.data() + (size_t)offset,
                             frame_bytes, sps_arg, pps_arg, annexb);

                if (s->current_sample < 3) {
                    H264_LOG("[h264] frame %d: off=%lld bytes=%u annexb=%zu\n",
                             s->current_sample, (long long)offset,
                             frame_bytes, annexb.size());
                }

                unsigned char* planes[3] = {nullptr, nullptr, nullptr};
                SBufferInfo info = {};

                DECODING_STATE state = s->h264_dec->DecodeFrameNoDelay(
                    annexb.data(), (int)annexb.size(), planes, &info);

                s->current_sample++;

                if (s->current_sample < 5 || info.iBufferStatus == 1) {
                    H264_LOG("[h264]   f%d state=0x%x iBufferStatus=%d w=%d h=%d\n",
                             s->current_sample - 1, (int)state,
                             info.iBufferStatus,
                             info.UsrData.sSystemBuffer.iWidth,
                             info.UsrData.sSystemBuffer.iHeight);
                }

                if (state != dsErrorFree) {
                    H264_LOG("[h264]   decode state error: 0x%x\n", (int)state);
                }

                if (info.iBufferStatus == 1 && planes[0] && planes[1] && planes[2]) {
                    int w = info.UsrData.sSystemBuffer.iWidth;
                    int h = info.UsrData.sSystemBuffer.iHeight;
                    int strideY  = info.UsrData.sSystemBuffer.iStride[0];
                    int strideUV = info.UsrData.sSystemBuffer.iStride[1];

                    if (w > 0 && h > 0 && (w != s->width || h != s->height)) {
                        s->width = w;
                        s->height = h;
                        s->rgba_buffer_size = (size_t)w * h * 4;
                        free(s->rgba_buffer);
                        s->rgba_buffer = (uint8_t*)malloc(s->rgba_buffer_size);
                        if (!s->rgba_buffer) return JNI_FALSE;
                    }

                    yuv420_to_rgba(planes[0], strideY,
                                   planes[1], strideUV,
                                   planes[2], strideUV,
                                   w, h, s->rgba_buffer);
                    std::memcpy(dst, s->rgba_buffer, s->rgba_buffer_size);
                    return JNI_TRUE;
                }
            }
            return JNI_FALSE;
        }

        case VideoFormat::GIF: {
            if (index < 0 || index >= s->frame_count) return JNI_FALSE;
            uint8_t* p = s->gif_pixels +
                (size_t)index * s->width * s->height * 4;
            std::memcpy(dst, p, s->rgba_buffer_size);
            return JNI_TRUE;
        }

        default:
            return JNI_FALSE;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_v_akfz_cobe_nat_NativeVideoDecoder_close(
        JNIEnv*, jclass, jlong handle)
{
    auto* s = reinterpret_cast<DecoderState*>(handle);
    if (!s) return;
    if (s->plm)      plm_destroy(s->plm);
    if (s->h264_dec) WelsDestroyDecoder(s->h264_dec);
    if (s->h264_track >= 0) MP4D_close(&s->mp4);
    if (s->gif_pixels) stbi_image_free(s->gif_pixels);
    if (s->gif_delays) stbi_image_free(s->gif_delays);
    if (s->rgba_buffer) std::free(s->rgba_buffer);
    delete s;
}