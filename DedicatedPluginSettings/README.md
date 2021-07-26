# DedicatedPluginSettings - [Download](https://github.com/Vendicated/AliucordPlugins/blob/builds/DedicatedPluginSettings.zip?raw=true)

Adds a dedicated plugin settings category in the settings page right below Aliucord's settings.

![Screenshot](https://cdn.discordapp.com/attachments/852332951542956052/869345405861756948/Screenshot_20210727-002617_Aliucord.png)


### Submitting a plugin icon

If you would like to submit an icon for your plugin (Must be a discord drawable), please just dm me on Discord or something.

Alternatively, you may declare a field of type Drawable `pluginIcon` on your plugin and that will be used, e.g.:

```java
public class DedicatedPluginSettings extends Plugin {
    private Drawable pluginIcon;

    public void load(Context ctx) {
        pluginIcon = ContextCompat.getDrawable(ctx, R$d.ic_theme_24dp);
    }
}
```