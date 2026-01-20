# MassiveCraft - Monorepo
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

This is a fork of MassiveCraft's projects which is up to date with the latest Minecraft versions. This fork combines all of the necessary individual repositories into a single "monorepo" for easier compilation by anyone who wishes to compile the source themselves. 

The guide below contains basic instructions on how to compile MassiveCore, MassiveBooks, MassiveHat, CreativeGates, Factions, and FactionsChat.

Alternatively, you can download compiled versions [from the releases page](https://github.com/JeremyFail/MassiveCraft/releases).

# MassiveCraft Compilation Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Setup](#setup)
3. [Clone](#clone)
4. [Compile](#compile)
5. [Other Noteworthy Stuff](#other-noteworthy-stuff)

## Prerequisites
To compile the plugins contained in this repo, the following are required:
- Git installed
- Java installed
- Apache Maven installed
- Additional plugins downloaded for Maven (see below)

### Additional Plugins
Because some of the MassiveCraft plugins integrate with other third-party plugins, you will need to download these plugins manually in order to compile the source, as these plugins are not available via a Maven Repository to automatically download. The following plugins require manual installation via Maven to compile the source - check the version to download in MassiveSuper's `pom.xml` file:
- Dynmap
- LWC

## Setup
First, determine where you would like to have your MassiveCraft code project located on your computer. It could be on your desktop, users folder, documents, etc. The location of this directory does not matter - place it anywhere you like!

## Clone
Clone/Download this repository from GitHub. Usage of Git is beyond the scope of this tutorial, but there are many free guides available online. If you don't want to use Git, you can also just download the source as a Zip file, extract it, and compile the source from the extracted contents.

## Compile
To compile the project, open the MassiveCraft directory where your code was cloned/downloaded to in your terminal program of choice. Run the command `mvn clean install`. This will then build the sub-repositories in the proper order and create JAR files that can be installed on your server.

It may take a few minutes to build. Once complete (and if successful), the JAR files will be located at:
```
../MassiveCore/target/MassiveCore.jar
../MassiveBooks/target/MassiveBooks.jar
../MassiveHat/target/MassiveHat.jar
../CreativeGates/target/CreativeGates.jar
../Factions/target/Factions.jar
../FactionsChat/target/FactionsChat.jar
```

## Other Noteworthy Stuff

### The version numbers must match
If you make changes to the code, the version numbers in each of the sub-repository `pom.xml` files must match. The version number in the root `MassiveCraft/pom.xml` can be 1.0.0 forever. However, the version numbers in the sub-repositories must all be in sync. Remember to update the version numbers at the same time if you make changes.
