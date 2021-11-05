const { promisify } = require("util");
const _exec = promisify(require("child_process").exec);
const fs = require("fs/promises");
const path = require("path");

async function main() {
    if (process.platform !== "linux") handleErr("This script only works on Linux.");
    const input = process.argv[2];
    const shouldResize = process.argv[3] === "true";
    const resolution = process.argv[4] || "72";

    await checkCommandExists("zip", "unzip");
    if (shouldResize) await checkCommandExists("convert");

    if (!input || !input.endsWith(".zip")) {
        handleErr(`Usage: ${process.argv[0]} ${process.argv[1]} [EMOJI_ZIP] [--resize RESOLUTION]`);
    }

    if (!(await exists(input))) handleErr("No such file: " + input);

    const workdir = await exec("mktemp -d");
    const extracted = path.join(workdir, "extracted");
    await fs.mkdir(extracted);
    await exec(`unzip -q ${input} -d ${extracted}`);

    const output = path.join(workdir, "output");
    await fs.mkdir(output);

    const data = require("./emojis.json");

    async function processEmoji({ surrogates, diversityChildren, parent }) {
        await Promise.all(diversityChildren?.map(c => processEmoji({ ...c, parent: surrogates })) ?? []);
        const codepoints = Array.from(surrogates).map(c => c.codePointAt(0).toString(16).padStart(4, "0"));
        let name = codepoints.join("-") + ".png";
        if (!(await exists(extracted, name))) {
            name = codepoints.filter(c => c !== "fe0f").join("-") + ".png";
            if (!(await exists(extracted, name))) {
                console.warn("Didn't find emoji " + surrogates);
                return;
            }
        }
        const outputName = `${parent ? `${parent}_` : ""}${surrogates}.png`;
        const inputFile = path.join(extracted, name);
        const outputFile = path.join(output, outputName);
        if (shouldResize) await exec(`convert ${inputFile} -resize ${resolution}x${resolution} ${outputFile}`);
        else await fs.rename(inputFile, outputFile);
    }

    await Promise.all(Object.values(data).flat().map(processEmoji));
    const interface = require("readline").createInterface({
        input: process.stdin,
        output: process.stdout,
    });

    console.log("\nDONE! Please paste the license below then CTRL+C:\n");
    const lines = [];
    interface.on("line", line => lines.push(line));
    interface.on("close", async () => {
        await fs.writeFile(path.join(output, "LICENSE.txt"), lines.join("\n"));
        await exec(`cd ${output} && zip -D -q -r ${path.join(__dirname, "output.zip")} *`);
    });
    interface.prompt();
}

async function checkCommandExists(...commands) {
    return Promise.all(
        commands.map(async cmd => {
            await _exec(`which ${cmd}`).catch(() => handleErr(`${cmd} not found. Please install it and try again`));
        })
    );
}

function exists(directory, file = "") {
    file = path.join(directory, file);
    return fs
        .access(file)
        .then(() => true)
        .catch(() => false);
}

function handleErr(err) {
    console.error(err);
    process.exit(1);
}

function exec(command) {
    console.info("Running " + command);
    return _exec(command)
        .then(({ stdout, stderr }) => {
            if (stderr) handleErr(stderr);
            if (stdout) console.log(stdout);
            return stdout.trim();
        })
        .catch(handleErr);
}

main();
