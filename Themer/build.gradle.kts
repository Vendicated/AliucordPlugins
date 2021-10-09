version = "3.4.5"
description = "Apply custom themes to your Discord"

aliucord.changelog.set(
    """
    
    # 3.4.2
    * Discord 96.0 update
    * Font hook is now toggleable via a switch due to a bug that makes the font weird
    
    # 3.4.1
    * Make overlay_alpha work for animated backgrounds
    * Fix bug where settings pages would become darker every re-render

    # 3.4.0
    * Add animated wallpaper support (.gif only)
    
    # 3.3.1
    * Implement full transparency
    
    # 3.2.3
    * Fix theme updater
    * Editor: Don't show autocomplete by default
    * Editor: Fix changing color while they are filtered
    
    # 3.2.2
    * Editor: Fix bug when searching in "new" tab
    * Editor: Make "new" button sticky
    
    # 3.2.1
    * Theme Downloader: Support multiple themes in one message
    * Add i.ibb.co to allowed domains
    * Fix text being cut off in editor
    * Make editor validator more leniant 
    
    # 3.2.0
    * Add Material You support (Android 12 only) - [Learn more](https://material.io/blog/announcing-material-you) - [Colors](https://developer.android.com/reference/android/R.color#system_accent1_0)
    * Add Color and Drawable Viewer to the Theme Editor

    # 3.1.3
    * Improve font hook.
    
    # 3.1.2
    * Add allowed hosts section to editor
    * Fix url validator
    
    # 3.1.1
    * Editor: Make auto complete options have full width so they are more easily clickable
    * Editor: Fix bug when adding multiple colours
    * Editor: Add fonts tab
    
    # 3.1.0
    * Untrusted img/font hosts are no longer allowed. Use github, imgur or discord. Otherwise themes could be used as an IPLogger.
    * Implement background/font caching
    * Fix inconsistent font application
    * Add background overlay alpha variable to darken backgrounds (0-255, 0 = no overlay, 1-255 = black overlay with this alpha)
    * Add background blur variable (blur might look doodoo I will probably have to work on this more)
    * Fix background image getting squished when opening the keyboard
    * Add support for local backgrounds/fonts in the form of file uris (file:///full/path/to/file)
    
    # 3.0.0
    * Revamp theme format (see README for details)
    * Revamp Editor: Organise values into categories, improve colour picker, add variable name auto complete
    
    # 2.2.1
    * ThemeEditor: Make field validators allow making fields empty (field will be removed from theme)
    * New Theme: Fix double ##
    
    # 2.2.0
    * Add Theme editor / creator
    * Allow loading multiple themes at once
    
    # 2.1.0
    * Fix simple_accent_color
    * Revert simple_accent_color theming chat input, timestamps etc
    * simple colours now have less priority, meaning individual colours will always override them
    
    # 2.0.0
    * Rewrite from scratch
    * Theme more things
    * Add Transparency Modes
    * Remove "import theme" button. Please use the download feature (long press message in #themes) or move themes manually to Aliucord/themes
    * Add restart prompt when changing settings
    
""".trimIndent()
)
