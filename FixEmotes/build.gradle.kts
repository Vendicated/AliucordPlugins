version = "1.0.1"
description = "Fixes some emotes being unusable if you have two emotes with the same name but different casing"

aliucord.changelog.set("""
    # 1.0.1
    - Fix for Discord 120.11
    - Should now 100% accurately replace emotes with the correct emoji (and not a random emoji with that same name)
""".trimIndent())
