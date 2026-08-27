[English] | [Русский](README_RU.md)

# Cobe

**Cobe** is a library and animation engine for Minecraft designed to work with complex 3D models and meshes. Created as a more feature-rich alternative to GeckoLib.

## Key Features

- **Mesh and Skinning Support**: Works with arbitrary geometry (vertices, UV coordinates, faces) and mesh deformation based on bone weights (*Skinned Mesh Animation*).
- **Multithreaded Animation Calculation**: Bone matrix calculations run in a separate thread pool (`AsyncAnimationEngine`) with dynamic load balancing, eliminating FPS drops on the main render thread.
- **Advanced Animation System**:
    - Support for arbitrary frame rates (FPS) for each animation.
    - Layered playback (*Layers*) with bone mask support (*Bone Masks*).
    - Blending (*Blending*) and smooth transitions (*Fade In* / *Fade Out*).
    - Playback queueing (*Queue*) and holding on the last frame (*Hold on last frame*).
    - Set of interpolation functions: `Linear`, `Step`, `Bezier`, as well as Easing functions (`Bounce`, `Elastic`, `Exponential`, `Back`, `Circular`, etc.).
- **Procedural Modifications (Bone Modifiers)**: Ability to inject custom bone transformations in real time via the `BoneModifier` interface.
- **Video Textures**(optional): Built-in integration with FFmpeg/JavaCV (`VideoPlayerManager`) for streaming video files (`.mp4`) directly onto model texture maps (may be deprecated or modified in the future).
- **Dynamic Resource Loading**: Loading and parsing models and animations from JSON at runtime or via custom resource packs (`CobeCFGPack`).

**A [plugin](https://github.com/Akfz/Cobe_blenderPlugin) is required for Blender**

## Planned Features

- Add Rigid-body and Ragdoll physics 
- Update the animation system to allow changing frame rates during playback, and add IK (Inverse Kinematics).
