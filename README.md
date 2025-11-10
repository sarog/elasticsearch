[![FreeBSD Tests](https://github.com/portsbuild/elasticsearch/actions/workflows/freebsd-test.yml/badge.svg)](https://github.com/portsbuild/elasticsearch/actions/workflows/freebsd-test.yml)

# Elasticsearch on FreeBSD

## Overview

This project is an unofficial port of Elasticsearch for FreeBSD systems. It was created to continue supporting Elasticsearch on FreeBSD after Elastic [introduced NativeAccess](https://github.com/elastic/elasticsearch/pull/108970) in 8.16, making it difficult to run ES without additional source code modifications.

The following table lists the actively maintained releases on this repository. These versions are tested & supported on FreeBSD 14.3, and presumed to work on 13.5 and 15.x.

| ES    | Branch                                                                | Bugzilla                                                         |
|-------|-----------------------------------------------------------------------|------------------------------------------------------------------|
| 8.19* | [Link](https://github.com/portsbuild/elasticsearch/tree/freebsd-8.19) | [Link](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=288653) |
| 9.1   | [Link](https://github.com/portsbuild/elasticsearch/tree/freebsd-9.1)  | [Link](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=290096) |
| 9.2   | [Link](https://github.com/portsbuild/elasticsearch/tree/freebsd-9.2)  | TBA                                                              |

\* Current recommendation for production use.

## Installation

The recommended way to install Elasticsearch is by using the port makefile. Since this project's port is not included in the official FreeBSD package repositories or ports index, one can create a local repository and install from there. Currently, a new port makefile is posted on the [FreeBSD Bugzilla entry](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=288653) page as new Elasticsearch versions are released.

> [!WARNING]
> If you are upgrading an existing Elasticsearch instance to version 9.x, you **MUST** upgrade to 8.19 first!
> Read the [official documentation](https://www.elastic.co/docs/deploy-manage/upgrade#upgrade-paths) for additional info.

### New installations

If you are upgrading from a previous version (e.g. 8.11.3) then skip to the [upgrading](#upgrading-existing-installations) section below.

```shell
mkdir /usr/local/ports
cd /usr/local/ports
fetch "https://bugs.freebsd.org/bugzilla/attachment.cgi?id=264852" -o /usr/local/ports/elasticsearch-8.19.6.tar.gz
tar xvf elasticsearch-8.19.6.tar.gz
cd textproc/elasticsearch8
make install clean
sysrc elasticsearch_enable="YES"
service elasticsearch start
```

### Upgrading existing installations

This method can upgrade older versions of Elasticsearch, such as the outdated one found in the [FreeBSD Ports index](https://www.freshports.org/textproc/elasticsearch8/). It is highly recommended to read the [official upgrade documentation](https://www.elastic.co/docs/deploy-manage/upgrade/deployment-or-cluster/self-managed) before proceeding.

> [!WARNING]
> If you are upgrading an existing Elasticsearch instance to version 9.x, you **MUST** upgrade to 8.19 first!
> Read the [official documentation](https://www.elastic.co/docs/deploy-manage/upgrade#upgrade-paths) for additional info.

```shell
mkdir /usr/local/ports
cd /usr/local/ports
fetch "https://bugs.freebsd.org/bugzilla/attachment.cgi?id=264852" -o /usr/local/ports/elasticsearch-8.19.6.tar.gz
tar xvf elasticsearch-8.19.6.tar.gz
cd textproc/elasticsearch8
make reinstall clean
```

Update `/usr/local/etc/elasticsearch/jvm.options` by adding the following entries at the bottom of the file:

```shell
-Des.nativelibs.path=/usr/local/lib
-Dorg.elasticsearch.nativeaccess.enableVectorLibrary=false
```

Next, ensure Elasticsearch is configured to use OpenJDK 21 by modifying `/etc/rc.conf`:

```shell
elasticsearch_java_home="/usr/local/openjdk21"
```

Finally, start the service:

```shell
service elasticsearch start
```

## Building

Building Elasticsearch is fairly straightforward. A FreeBSD-specific archive can be built on any operating system thanks to Java's cross-compilation capabilities. [Building the vector library](#building-the-vector-library) natively requires a FreeBSD host.

### Prerequisites

Install the necessary JDKs and other build dependencies to compile and run Elasticsearch.

> [!NOTE]
> As of 2025-11-10, OpenJDK 25 is only available on the FreeBSD _latest_ pkg repo.

```shell
pkg install bash curl protobuf gcc13 java/openjdk17 java/openjdk19 java/openjdk20 java/openjdk21 java/openjdk22 java/openjdk23 java/openjdk25
```

Clone this repository by either checking out a release branch such as `freebsd-8.19` or a specific tag, e.g. `8.19.6`:

```shell
git clone --depth 1 --branch v8.19.6 https://github.com/portsbuild/elasticsearch elasticsearch-8.19.6
cd elasticsearch-8.19.6
```

Set the default JDK to 25 and begin the build:

```shell
export RUNTIME_JAVA_HOME=/usr/local/openjdk25
./gradlew distribution:archives:freebsd-tar:assemble -D"build.snapshot=false" -D"license.key=public.key" -Porg.gradle.java.installations.paths=/usr/local/openjdk17,/usr/local/openjdk19,/usr/local/openjdk20,/usr/local/openjdk21,/usr/local/openjdk22,/usr/local/openjdk23,/usr/local/openjdk25
```

Copy the freshly built archive to `/usr/ports/distfiles`:

```shell
mv distribution/archives/freebsd-tar/build/distributions/elasticsearch-8.19.6-freebsd-x86_64.tar.gz /usr/ports/distfiles/
```

Create a `/usr/local/ports` directory and extract the port patch/makefile there:

```shell
mkdir -p /usr/local/ports
cd /usr/local/ports/textproc/elasticsearch8
fetch "https://bugs.freebsd.org/bugzilla/attachment.cgi?id=264852" -o /usr/local/ports/elasticsearch-8.19.6.tar.gz
make makesum
make install clean
```

### Building the vector library

Compiling the vector library is simple. From the root of the repository:

```shell
cd libs/simdvec/native/src/vec
clang -shared -fpic -o libvec.so -I headers/ c/amd64/vec.c -O3 -march=core-avx2 -Wno-incompatible-pointer-types
```

Next, copy `libvec.so` to `/usr/local/lib`:

```shell
cp libvec.so /usr/local/lib/
```

Finally, set `enableVectorLibrary` to true in `jvm.options` and (re)start Elasticsearch:

```ini
-Dorg.elasticsearch.nativeaccess.enableVectorLibrary=true
```

```shell
service elasticsearch (re)start
```

## Testing

A [separate branch](https://github.com/portsbuild/elasticsearch/tree/freebsd-tests) has been created to maintain FreeBSD tests. The decision to keep the tests separate was to avoid cluttering up the release branches. This also eases keeping track of changes between releases.

To run the full suite of tests, switch over to the `freebsd-tests` branch and type:

```shell
export RUNTIME_JAVA_HOME=/usr/local/openjdk25
./gradlew test -D"tests.haltonfailure=false" -D"build.snapshot=false" -D"license.key=public.key" -D"run.license_type=trial" -Porg.gradle.java.installations.paths=/usr/local/openjdk17,/usr/local/openjdk19,/usr/local/openjdk20,/usr/local/openjdk21,/usr/local/openjdk22,/usr/local/openjdk23,/usr/local/openjdk25
```

For the vector library benchmarks, a copy of `libzstd.so` and `libvec.so` (see [build instructions](#building-the-vector-library)) are required.

```shell
## From the repository root:
mkdir -p libs/native/libraries/build/platform/freebsd-x64
cp libs/vec/shared/amd64/libvec.so libs/native/libraries/build/platform/freebsd-x64/
cp /usr/local/lib/libzstd.so libs/native/libraries/build/platform/freebsd-x64/
export RUNTIME_JAVA_HOME=/usr/local/openjdk25
./gradlew -p benchmarks run --args 'Int7uScorerBenchmark' -Porg.gradle.java.installations.paths=/usr/local/openjdk17,/usr/local/openjdk19,/usr/local/openjdk20,/usr/local/openjdk21,/usr/local/openjdk22,/usr/local/openjdk23,/usr/local/openjdk25 -D"--enable-native-access=ALL-UNNAMED"
```

## Kibana, Logstash & Beats

Administrators may be interested in the following related components when running the ELK stack:

- [Kibana 8.19](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=289759)
- [Beats 8.19](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=290094)
- [Logstash 8.19](https://bugs.freebsd.org/bugzilla/show_bug.cgi?id=290095)

## Support

For technical assistance or to report a bug, please [create a GitHub Issue](https://github.com/portsbuild/elasticsearch/issues/new/choose).

## Misc.

The original [Elasticsearch README](README.ORIG.asciidoc) is also available.
