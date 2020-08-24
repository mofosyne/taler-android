# GNU Taler Point of Sale merchant terminal

This merchant point of sale terminal Android app allows sellers to

* process customers’ orders by adding or removing products,
* calculate the amount owed by the customer and
* let the customer make a Taler payment via QR code or NFC.

## Building

You can import the project into Android Studio
or build it with Gradle on the command line (from the repository root):

    $ ./bootstrap
    $ ./gradlew :merchant-terminal:build

More information can be found in the
[Taler developer manual](https://docs.taler.net/developers-manual.html#build-apps-from-source).
