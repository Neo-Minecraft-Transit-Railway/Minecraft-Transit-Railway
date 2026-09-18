## Minecraft Transit Railway 4.0.5

_Minecraft Transit Railway_ is a [Minecraft mod](https://minecraft.wiki/w/Mods) that allows you to build your own transport network with automated trains, boats, cable cars, and planes!

This workspace targets the Fabric port for Minecraft 1.21.11. The vehicle simulation code is separated from the Minecraft instance, reducing lag on Minecraft servers.

## Major Features

- Custom train configuration
- Realistic bogies and train movement
- Improved signalling
- Sideways elevators
- OBJ features ported over from Nemo's Transit Expansion
- Vehicle swaying effect
- New online system map and OneBusAway integration
- Client-side FPS improvements and server-side TPS improvements

[See these features in detail.](https://www.youtube.com/playlist?list=PLp0jZgheSlXuQCXu9UynrKuBnq2Ef_TSI)

[![Video Trailer](https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/video-preview.png)](https://www.youtube.com/watch?v=1cZfU7t4cAk)

Please report any issues or bugs you find; that would be greatly appreciated! Refer to the [todo list](https://github.com/jonafanho/Minecraft-Transit-Railway/projects/2) to see currently known issues.

## Downloads and Installation

Head over to the [Modrinth page](https://modrinth.com/mod/minecraft-transit-railway) to download the mod or to see the project information.

## Guide

There is a [new wiki](https://wiki.minecrafttransitrailway.com/start) for the mod. Take a look.

## Recommended Addons

- [MTR London Underground Addon](https://modrinth.com/mod/the-tube)
- [Joban Client Mod](https://modrinth.com/mod/jcm)
- [Station Decoration](https://modrinth.com/mod/station-decoration)
- [Russian Metro Addon](https://modrinth.com/mod/russian-metro-addon)

## Contributing

### Help Translate the Mod!

The [Crowdin site for the Minecraft Transit Railway mod](https://crwd.in/minecraft-transit-railway) is available!

Crowdin is a cloud-based platform for translators to contribute to a project. With your help, we can translate the mod into many different languages. You may create a free account to start translating.

[![Crowdin](https://badges.crowdin.net/minecraft-transit-railway/localized.svg)](https://crowdin.com/project/minecraft-transit-railway)

### Adding Features

1. Fork this project
2. On your fork, create a new branch based on the development version branch
3. Commit your changes to the new branch
4. Make a Pull Request to merge your branch into the development version of this repository

### Building

This checkout targets **Minecraft 1.21.11 / Fabric / MTR 4.0.5 only**. Use Java 21 or newer, Node.js with npm, the local dependency jars in `libs/`, and the sibling `../Minecraft-Mappings/` checkout with its 1.21.11 generated mixins. This is a multi-project porting workspace, not a standalone upstream build.

Run these commands in the project root, in order. Compile the website before embedding it into the mod:

```sh
./gradlew :fabric:setupWebsiteFiles
npm ci --prefix website
npm run build --prefix website
./gradlew :fabric:setupFiles
./gradlew :fabric:build
```

After website-only changes, run the npm build and `./gradlew :fabric:setupWebserverResources :fabric:build`. Embedding is local and fails if `index.html` is missing, rather than generating an empty website.

See `docs/WORKSPACE.md` for the directory layout, addon build commands, audit scope, and maintenance results.

The mod jar file should be generated in the following directory:

```
<root>/build/release/MTR-fabric-4.0.5+1.21.11.jar
```

## License

This project is licensed under the [MIT License](https://opensource.org/licenses/MIT). All [Noto fonts](http://www.google.com/get/noto/), bundled with this mod, are licensed under the [Open Font License](http://scripts.sil.org/OFL).

## Questions? Comments? Complaints?

Let's connect.

<a href="https://discord.gg/PVZ2nfUaTW" target="_blank"><img src="https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/discord.png" alt="Discord" width=64></a>
&nbsp;
<a href="https://www.linkedin.com/in/jonathanho33" target="_blank"><img src="https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/linked_in.png" alt="LinkedIn" width=64></a>
&nbsp;
<a href="mailto:jonho.minecraft@gmail.com" target="_blank"><img src="https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/email.png" alt="Email" width=64></a>
&nbsp;
<a href="https://www.patreon.com/minecraft_transit_railway" target="_blank"><img src="https://github.com/jonafanho/Minecraft-Transit-Railway/blob/master/images/footer/patreon.png" alt="Patreon" width=64></a>
