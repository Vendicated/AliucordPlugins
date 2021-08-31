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
- Possibility for theme updates

Simply install the plugin and install a theme from the #themes channel on the Aliucord server by long pressing the message and clicking the context menu install button.

### Alternatively, create your own theme

Themes are most easily made by using the [Discord Themer](https://github.com/Aliucord/DiscordThemer), an app similar to this plugin. It requires Xposed but you can use it
without it to create themes for the themer plugin

### Theme spec (SUBJECT TO CHANGE)
- flat json file with key value pairs
- All keys are optional
- keys:
  - name: The name of the theme
  - author: The name of the theme author
  - version: The version of the theme (Format: 1.0.0) used for updating
  - updater: Url pointing to **raw** json file of your theme that will be updated, e.g. a github raw link
  - license: [SPDX License Identifier](https://spdx.org/licenses/)
  - background_url: Url of image to be used as background when transparency is enabled
  - background_transparency: Background opacity, number from 0 (fully transparent) to 255 (fully opaque)
  - simple_accent_color: Themes all accent colours
  - simple_bg_color: Themes all background colours
  - simple_bg_secondary_color: Themes all secondary background colours
  - mention_highlight: Themes the mention highlight
  - active_channel_color: Themes the overlay that is put over the currently selected channel
  - statusbar_color: Themes the statusbar colour (top bar with clock, notifications, wifi icon and such on it)
  - input_background_color: Themes the chat text input box
  - font: Url pointing to `.ttf` file that will replace the font in almost all places
  - font_* where * is the name of any discord font: Url pointing to `.ttf` file that will replace the respective font
  - color_* where * is any discord colour resource name: Themes the respective colour
  - drawablecolor_* where * is any discord drawable resource name: Tints the respective drawable
