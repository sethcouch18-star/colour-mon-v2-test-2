# Colormons 2.0 (Cobblemon 1.8.1, NeoForge 1.21.1)

## What is in this build
- Six colour groups: Primary, Secondary, Tertiary, Eyes, Outline, Custom.
- Primary/Secondary/Tertiary are the three biggest colours in the texture.
- Eyes and Outline are found from the texture, or from a mask if you supply one
  (assets/colormons/masks/<species>.png, red = 1 green = 2 blue = 3 yellow = Eyes).
- Custom starts empty.
- Finishes: Default, Glossy, Matte, Shiny.
- Four quick-save palettes in the corner, save with the Save button.
- "Dark px" toggle keeps near-black pixels untouched.
- The mod only opens on your own Pokemon until you allow others: edit
  config/colormons.json and set "allowOthers" to true.

## Not in this build yet
- The live 3D preview panel (the box on the left is reserved for it).
- Clicking pixels on the preview to move them between groups. Widen/Shrink
  buttons are stubs.
- Multiplayer.

Build with GitHub Actions, same as before. Put colormons-1.0.0.jar in the mods folder.
