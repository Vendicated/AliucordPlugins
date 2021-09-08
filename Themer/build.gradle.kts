version = "3.1.2"
description = "Apply custom themes to your Discord"

aliucord.changelog.set(
    """
    
    # Future Roadmap
    * Implement full transparency
    * Add Fonts tab to theme editor
    * Add background_overlay_color variable
    
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
