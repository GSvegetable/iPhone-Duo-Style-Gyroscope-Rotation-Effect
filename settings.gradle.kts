// 插件仓库与依赖仓库的统一声明
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// 统一管理依赖仓库，禁止模块自己声明
// 好处：避免不同模块拉到同一库的不同源，产生诡异的版本冲突
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TiltFold"
include(":app")
