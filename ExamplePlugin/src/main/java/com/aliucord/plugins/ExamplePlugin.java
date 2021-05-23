package com.aliucord.plugins;

import android.content.Context;

import androidx.annotation.NonNull;

import com.aliucord.Main;
import com.aliucord.entities.Plugin;

import java.util.*;

@SuppressWarnings("unused")
public class ExamplePlugin extends Plugin {
    @NonNull
    @Override
    public Manifest getManifest() {
        Manifest manifest = new Manifest();
        manifest.authors = new Manifest.Author[]{ new Manifest.Author("DISCORD USERNAME", 123456789L) };
        manifest.description = "Example Plugin.";
        manifest.version = "1.0.0";
        manifest.updateUrl = "https://raw.githubusercontent.com/USERNAME/REPONAME/builds/updater.json";
        return manifest;
    }
    public static Map<String, List<String>> getClassesToPatch() {
        Map<String, List<String>> map = new HashMap<>();
        // map.put("com.discord.something.that.you.want.to.patch", Collections.singletonList("methodToPatch"));
        return map;
    }

    @Override
    public void start(Context context) {
        // patcher.patch("com.discord.something.you.want.to.patch", "methodName", (_this, args, ret) -> {
        //     do some stuff
        //
        //     return ret;
        // });
    }

    @Override
    public void stop(Context context) {
        // patcher.unpatchAll();
    }
}
