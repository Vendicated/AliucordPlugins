version = "3.6.5"
description = "Apply custom themes to your Discord"

aliucord.changelog.set(
    """
    # 3.6.5
    * Fix theme editor crashing when themes use Android 12 Material You (system_) dynamic colors, hexadecimal strings (#FF..., 0xFF...), or color names
    
    # 3.6.4
    * Fix colour theming for some elements like user profile header and chatbox

    # 3.6.3
    * Fix colour theming for newer android versions

    # 3.6.2
    * Now prompts to switch to dark mode if using light/pureEvil theme
    
    # 3.6.1
    * Re-enabled custom fonts. They may still be unstable, so use at your own risk  
""".trimIndent()
)
