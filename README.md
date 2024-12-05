# Demonstrate mapping only the produced object files from native compile tasks

Gradle does not expose the produced object files from a native compile tasks.
Instead, developers needs to perform error-prone mapping like the following:

```
def objectFiles = objects.fileTree()
		.dir(tasks.named('...', CppCompile).flatMap { it.objectFileDir }))
		.include('**/*.o', '**/*.obj')
```

If two tasks share the same object directory; or worst, stale outputs makes its way into the object directory, we will glob the wrong `objectFiles`.

## Solution

We map a `FileTree` with the exact logic Gradle uses to map its source with the object file.
This sample demonstrate how to do that.
Note that we rely on internal API usage for this work.

## Demonstration

```
$ ./gradlew assembleRelease

> Task :app:linkRelease
./app/build/obj/main/release/c0rn65lkroj8p4fvz0qgmzzjr/app.o

BUILD SUCCESSFUL
$ mkdir -p app/build/obj/main/release/somefakehashtoproveapoint/
$ echo "some fake obj file" > app/build/obj/main/release/somefakehashtoproveapoint/my-fake.obj
$ ./gradlew assembleRelease

> Task :app:linkRelease FAILED
./app/build/obj/main/release/somefakehashtoproveapoint/my-fake.obj
./app/build/obj/main/release/c0rn65lkroj8p4fvz0qgmzzjr/app.o
[...]

BUILD FAILED
$ git apply-patch use-plugin.patch
$ ./gradlew assembleRelease

> Task :app:linkRelease
./app/build/obj/main/release/c0rn65lkroj8p4fvz0qgmzzjr/app.o

BUILD SUCCESSFUL
$ find ./app/build/obj/main/release -type f
./app/build/obj/main/release/somefakehashtoproveapoint/my-fake.obj
./app/build/obj/main/release/c0rn65lkroj8p4fvz0qgmzzjr/app.o
```
