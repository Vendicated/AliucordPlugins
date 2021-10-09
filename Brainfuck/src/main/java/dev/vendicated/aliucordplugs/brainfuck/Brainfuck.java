/*
 * Ven's Aliucord Plugins
 * Copyright (C) 2021 Vendicated
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package dev.vendicated.aliucordplugs.brainfuck;

import android.content.Context;
import android.net.Uri;

import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.entities.CommandContext;
import com.aliucord.entities.Plugin;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.api.premium.PremiumTier;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

@AliucordPlugin
public class Brainfuck extends Plugin {
    @Override
    public void start(Context context) {
        var toBrainfuckOptions = Arrays.asList(
                Utils.createCommandOption(ApplicationCommandType.STRING, "text", "Ascii text to convert to brainfuck", null, true, true),
                Utils.createCommandOption(ApplicationCommandType.BOOLEAN, "send", "Whether to send the result visible for everyone")
        );

        var brainfuckOptions = Arrays.asList(
                Utils.createCommandOption(ApplicationCommandType.STRING, "brainfuck", "Brainfuck to evaluate", null, true, true),
                Utils.createCommandOption(ApplicationCommandType.STRING, "stdin", "Stdin for comma operator"),
                Utils.createCommandOption(ApplicationCommandType.BOOLEAN, "send", "Whether to send the result visible for everyone")
        );

        commands.registerCommand("tobrainfuck", "Converts plain ascii text to brainfuck", toBrainfuckOptions, ctx -> {
            var text = ctx.getRequiredString("text");
            var res = toBrainfuck(text);

            return sendResult(context, ctx, String.format("```bf\n%s```", res), res, "brainfuck.bf");
        });

        commands.registerCommand("brainfuck", "Interprets brainfuck", brainfuckOptions, ctx -> {
            var brainfuck = ctx.getRequiredString("brainfuck");
            var stdin = ctx.getString("stdin");
            if (brainfuck.contains(",") && stdin == null)
                return new CommandsAPI.CommandResult("Brainfuck contains comma operator but no argument was specified for stdin.", null, false);

            var reasonRef = new AtomicReference<String>();
            if (!validateBrainfuck(brainfuck, reasonRef))
                return new CommandsAPI.CommandResult(reasonRef.get(), null, false);

            try {
                var res = evaluateBrainfuck(brainfuck, stdin);
                return sendResult(context, ctx, String.format("```\n%s```", res), res, "brainfuck-result.txt");
            } catch (IllegalArgumentException ex) {
                return new CommandsAPI.CommandResult("Brainfuck tried to read more chars than specified by stdin.", null, false);
            }
        });
    }

    private CommandsAPI.CommandResult sendResult(Context context, CommandContext ctx, String content, String fileContent, String fileName) {
        if (fileContent.isEmpty()) return new CommandsAPI.CommandResult("No output! Nothing here but us chicken...", null, false);
        boolean send = ctx.getBoolOrDefault("send", false);
        if (!send) return new CommandsAPI.CommandResult(content, null, false);
        int maxLen = ctx.getMe().getPremiumTier() == PremiumTier.TIER_2 ? 4000 : 2000;
        if (content.length() <= maxLen) return new CommandsAPI.CommandResult(content, null, true);
        try {
            var tempFile = File.createTempFile("brainfuck", null, context.getCacheDir());
            try (var fos = new FileOutputStream(tempFile)) {
                fos.write(fileContent.getBytes(StandardCharsets.UTF_8));
            }
            tempFile.deleteOnExit();
            ctx.addAttachment(Uri.fromFile(tempFile).toString(), fileName);
            final String mindBlownEmoji = "\uD83E\uDD2F";
            return new CommandsAPI.CommandResult(mindBlownEmoji, null, true);
        } catch (IOException e) {
            return new CommandsAPI.CommandResult("Sorry, that was too long :(", null, false);
        }
    }

    private String repeat(char c, int times) {
        var chars = new char[times];
        Arrays.fill(chars, c);
        return new String(chars);
    }

    private String toBrainfuck(String text) {
        var sb = new StringBuilder();
        char current = 0;
        for (char c : text.toCharArray()) {
            char sign = c > current ? '+' : '-';
            int diff = Math.abs(c - current);
            if (diff == 0) {
                sb.append('.');
            } else if (diff < 10) {
                sb.append(repeat(sign, diff)).append('.');
            } else {
                int closestSqn = (int) Math.round(Math.sqrt(diff));
                int rest = (int) (diff - (Math.pow(closestSqn, 2)));

                sb
                        .append('>')
                        .append(repeat('+', closestSqn))
                        .append("[<")
                        .append(repeat(sign, closestSqn))
                        .append(">-]<")
                        .append(repeat(rest > 0 ? sign : sign == '-' ? '+' : '-', Math.abs(rest)))
                        .append('.');
            }

            current = c;
        }

        return sb.toString();
    }

    private boolean validateBrainfuck(String bf, AtomicReference<String> reasonRef) {
        int depth = 0;
        int lastOpen = 0;
        int lastClose = 0;
        for (int i = 0; i < bf.length(); i++) {
            switch (bf.charAt(i)) {
                case '[':
                    lastOpen = i;
                    depth++;
                    break;
                case ']':
                    lastClose = i;
                    depth--;
                    break;
            }
            if (depth < 0) break;
        }

        boolean isValid = depth == 0;
        if (!isValid) {
            boolean isUnclosed = depth > 0;
            int position = isUnclosed ? lastOpen : lastClose;
            int startIdx = Math.max(0, position - 5);
            int endIdx = Math.min(startIdx + 11, bf.length());
            reasonRef.set(String.format("Illegal brainfuck: Unmatched %s at position %s: %s", isUnclosed ? '[' : ']', position, bf.substring(startIdx, endIdx)));
        }
        return isValid;
    }

    private String evaluateBrainfuck(String bf, String stdin) throws IllegalArgumentException {
        // ram usage go brrrrr but at least 30k is required as per official spec
        // actually nvm at worst condition (4 bytes per int) it only uses 0.1MB ram and java
        // probably realises these are all 0 and optimises idk
        int[] buffer = new int[30000];
        var sb = new StringBuilder();

        for (int cursor = 0, pointer = 0, stdinIdx = 0; cursor < bf.length(); cursor++) {
            switch (bf.charAt(cursor)) {
                case '>':
                    if (pointer == buffer.length - 1)
                        pointer = 0;
                    else
                        pointer++;
                    break;
                case '<':
                    if (pointer == 0)
                        pointer = buffer.length - 1;
                    else
                        pointer--;
                    break;
                case '+':
                    buffer[pointer]++;
                    break;
                case '-':
                    buffer[pointer]--;
                    break;
                case '.':
                    sb.append((char) buffer[pointer]);
                    break;
                case ',':
                    try {
                        buffer[pointer] = stdin.charAt(stdinIdx++);
                    } catch (IndexOutOfBoundsException ex) { throw new IllegalArgumentException(); }
                    break;
                case '[': {
                    if (buffer[pointer] == 0) {
                        int depth = 1;
                        do {
                            switch (bf.charAt(++cursor)) {
                                case '[':
                                    depth++;
                                    break;
                                case ']':
                                    depth--;
                                    break;
                            }
                        } while (depth != 0);
                    }

                    break;
                }
                case ']': {
                    int depth = 0;
                    do {
                        switch (bf.charAt(cursor--)) {
                            case '[':
                                depth--;
                                break;
                            case ']':
                                depth++;
                                break;
                        }
                    } while (depth != 0);

                    break;
                }
            }
        }
        
        return sb.toString();
    }

    @Override
    public void stop(Context context) {
        commands.unregisterAll();
    }
}
