# GNU Taler Android Code Repository

This git repository contains code for GNU Taler Android apps and libraries.
The official location is: 

    https://git.taler.net/taler-android.git
    
## Structure

* [**anastasis-ui**](/anastasis-ui) - an Android library for sync and backup UI.
  See [Anastasis](https://docs.taler.net/anastasis.html).
* [**cashier**](/cashier) - an Android app that enables you to take cash and give out electronic cash
* [**merchant-lib**](/merchant-lib) - a library providing communication with a merchant backend
  to be used by the point of sale app below.
* [**merchant-terminal**](/merchant-terminal) - a merchant point of sale terminal Android app
  that allows sellers to
  process customers’ orders by adding or removing products,
  calculate the amount owed by the customer
  and let the customer make a Taler payment via QR code or NFC.
* [**taler-kotlin-android**](/taler-kotlin-android) - an Android library containing common code
  needed by more than one Taler Android app.
* [**multiplatform**](/multiplatform) - multi-platform Taler libraries
  included as a [git submodule](https://git-scm.com/docs/git-submodule)
  from [`https://git.taler.net/wallet-kotlin.git`](https://git.taler.net/wallet-kotlin.git/)
* [**wallet**](/wallet) - a GNU Taler wallet Android app

## Building

Before building anything, you should initialize and update the submodules by running
    
    $ ./bootstrap
    
Then, you can get a list of possible build tasks like this:
    
    $ ./gradlew tasks
    
See the [Taler developer manual](https://docs.taler.net/developers-manual.html#build-apps-from-source).
for more information about building individual apps.
