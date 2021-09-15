# Themer - [Download](https://github.com/Vendicated/AliucordPlugins/blob/builds/Themer.zip?raw=true)

Custom Themes!

Note: This plugin is quite heavy. If your Aliucord becomes slow, it may be why. If you are rooted, consider using
the [Xposed Discord Themer](https://github.com/Aliucord/DiscordThemer) instead.

### Features
- Custom colours
- Custom icon (drawable) colours
- Optional transparency with a custom background wallpaper (may be laggy on older devices)
- Custom fonts
- Themes are single json files
- Theme auto updates
- Fully featured Theme Editor

Simply install the plugin and install a theme from the #themes channel on the Aliucord server by long pressing the message and clicking the context menu install button.

### Alternatively, create your own theme

Simply open the plugin settings, click new theme and go crazy! Check the pins in the #theme-development for help.

### Example Theme

⚠️ Requires you to enable "Full Transparency" in the plugin settings

[Download](https://cdn.discordapp.com/attachments/852332951542956052/887844412745482310/Firefly.json) | [Source](https://raw.githubusercontent.com/Vendicated/AliucordPlugins/main/Themer/Firefly.json)

![](https://cdn.discordapp.com/attachments/852332951542956052/887844097367359528/Screenshot_20210916-010358.png)
![](https://cdn.discordapp.com/attachments/852332951542956052/887844110860431450/Screenshot_20210916-010234.png)
![](https://cdn.discordapp.com/attachments/852332951542956052/887844126178021426/Screenshot_20210916-010151.png)
![](https://cdn.discordapp.com/attachments/852332951542956052/887844139780161536/Screenshot_20210916-010204.png)

### Theme spec (SUBJECT TO CHANGE)
```json
{
  "manifest": {
    "name": "Awesome Theme",
    "version": "1.0.0",
    "author": "Ven",
    "license": "Unlicense",
    "updater": "url to raw json file, will be used to update your theme"
  },
  "simple_colors": {
    "some_color_name": -1371931
  },
  "background": {
    "url": "background-url",
    "overlay_alpha": 150,
    "blur_radius": 1.7
  },
  "fonts": {
    "*": "everywhere-font-url",
    "ginto_bold": "ginto-replacement-url"
  },
  "colors": {
    "brand_new": -13978120,
  },
  "drawableTints": {
    "drawable_overlay_channels_selected_dark": -131020192,
  }
}
```
