# AcidVisuals Wet Floor — Minecraft 1.21.4 / Fabric

This is a standalone client-side subproject. The original root project and its files are left intact.

## Build and download
GitHub Actions builds the project from this folder, uploads the JAR as an Actions artifact, and publishes a versioned GitHub Release asset. Targets: Java 21, Minecraft 1.21.4, Yarn build 8 and Fabric API 0.119.2+1.21.4.

## Included
- Right Shift glass-style menu with tabs, rounded high-resolution bundled textures and a supersampled bitmap font atlas generated during the build.
- Saved toggles, sliders, accent color and three presets in the config file.
- Jump Circle, glowing particles around the player and the aimed-at entity, entity outlines and name/distance labels.
- Custom rounded hotbar overlay with a smoothly moving slot-selection frame.
- Stylized wet surface overlays and rain-driven ripple rings, plus a subtle high-resolution atmosphere overlay.

## Important rendering note
Wet Floor is a client-side stylized water/reflection effect. It is not hardware ray tracing or a full scene-reflection renderer: it does not reconstruct exact mirror images of every block and entity like a dedicated shader pack or a custom framebuffer renderer would. No Minecraft commands are sent; the visual features do not require this mod to be installed on the server.
