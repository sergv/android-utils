Overview
========
These are various utilities for writing Android applications with Clojure.

Build Process
=============
To build this library make sure that [Android SDK](http://developer.android.com/sdk/index.html),
[Leiningen](http://leiningen.org) and [lein-droid plugin](https://github.com/clojure-android/lein-droid)
are installed and create `project-key.clj` file containing

```clojure
{:sdk-path ... ;; path to location where android sdk is installed, hopefully should be the same as SDK_HOME environment variable
}
```

or add `:sdk-path <dir>` entry directly to `project.clj` under `:android` key.
Then issue

```shell
lein do droid-compile, install
```

which should install `android-utils` to your local maven repository.

