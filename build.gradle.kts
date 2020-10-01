plugins {
    kotlin("multiplatform") version "1.4.10"
}
group = "me.uloplt"
version = "0.0.1"

repositories {
    mavenCentral()
}

val mingwPath =
    File(System.getenv("MINGW64_DIR") ?: "C:/msys64/mingw64/")

kotlin {
    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val microuiTarget = when {
        hostOs == "Mac OS X" -> macosX64("microui")
        hostOs == "Linux" -> linuxX64("microui")
        isMingwX64 -> mingwX64("microui")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    microuiTarget.apply {
        binaries {
            executable {
                entryPoint = "main"
                when {
                    hostOs == "Mac OS X" -> linkerOpts("-L/opt/local/lib", "-L/usr/local/lib", "-lSDL2")
                    hostOs == "Linux" -> linkerOpts("-L/usr/lib64", "-L/usr/lib/x86_64-linux-gnu", "-lSDL2")
                    isMingwX64 -> linkerOpts(
                        "-L${mingwPath.resolve("lib")}",
                        "-Wl,-Bstatic",
                        "-lstdc++",
                        "-static",
                        "-lSDL2",
                        "-limm32",
                        "-lole32",
                        "-loleaut32",
                        "-lversion",
                        "-lwinmm",
                        "-lsetupapi",
                        "-mwindows"
                    )
                }
            }
        }

        val main by compilations.getting
        val SDL by main.cinterops.creating {
            when {
                hostOs == "Mac OS X" -> includeDirs("/opt/local/include/SDL2", "/usr/local/include/SDL2")
                hostOs == "Linux" -> includeDirs("/usr/include/SDL2")
                isMingwX64 -> includeDirs(mingwPath.resolve("include/SDL2"))
            }
        }
    }
    sourceSets {
        val microuiMain by getting
    }
}