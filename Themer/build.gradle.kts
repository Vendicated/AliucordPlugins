version = "3.0.0"
description = "Apply custom themes to your Discord"

aliucord.changelog.set(
    """
    
    # Future Roadmap
    * Implement full transparency
    
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
