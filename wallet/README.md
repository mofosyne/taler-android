# GNU Taler Wallet

This package implements a GNU Taler wallet for Android.
It is currently a UI for the wallet writen in TypeScript.

## Building

Currently, building the wallet for Android requires manually copying a dependency:

`taler-wallet-android.js` -> `src/main/assets/taler-wallet-android.js`

After that, the Android wallet can be built with Gradle:

    $ ./gradlew build

## Obtaining Dependencies

There are two ways of obtaining the dependencies.  The easiest one is
to use the pre-built versions, which are stored in the "binary-deps"
branch of the git://git.taler.net/wallet-android.git repository.

An easy way to access them is using a git worktree:

    $ git fetch origin binary-deps
    $ git worktree add binary-deps binary-deps
    $ cp binary-deps/taler-wallet-android.js src/main/assets/taler-wallet-android.js
    $ git worktree remove binary-deps

Alternatively, you can build it yourself from the repository:

 * git://git.taler.net/wallet-core.git
