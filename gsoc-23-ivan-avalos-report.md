[Link to the Android source code](https://git.taler.net/taler-android.git/tree/anastasis?h=dev/ivan-avalos/anastasis)

# Goals of the project

Anastasis is a key recovery system that allows users to create
encrypted backups of their keys or secrets, split across multiple
service providers, that can be recovered without needing a
password. The keys are protected using a key derived from the identity
attributes of the user, and can only be recovered by answering a
series of verification steps defined by the user.

The main goal of this Google Summer of Code project was to integrate
Anastasis into the Taler Android wallet, in order to allow users to
create encrypted and distributed backups of their wallet keys. This
could only be achieved by either implementing Anastasis functionality
within the wallet app, or creating a separate Anastasis app and
enabling integration between the two apps.

# What I did

I went for the second option and created a separate Anastasis app,
which has the advantage of allowing Anastasis to be easily used for
other use cases that don't involve the wallet. Besides, it gave me the
freedom to build the app with a more suitable architecture.

The core functionality for both the wallet and the Anastasis client is
built into the same component: “wallet-core”, written in
TypeScript. The Taler mobile apps and the web extension communicate
with wallet-core using a consistent and well-documented JSON API.

* [wallet-core.git](https://git.taler.net/wallet-core.git)
* [wallet-core API docs](https://docs.taler.net/wallet/wallet-core.html)

In order to run wallet-core in the mobile apps, a TypeScript runtime
needs to be embedded. There are many embedable runtimes, but some of
them are too big, and not all of them are optimal for the purposes of
Taler. QuickJS was chosen because of its small size and footprint,
while having good standards compatibility (ES2020).

* [QuickJS](https://bellard.org/quickjs/)

The Taler developers then created a cross-platform library built on
top of QuickJS, tailor-made for running wallet-core in mobile, called
“quickjs-tart” (nicknamed “qtart,” which stands for “**Q**uickJS
**TA**ler **R**un**T**ime”). This library implements native QuickJS
modules for cryptographic functions used by wallet-core, pre-compiles
wallet-core to C, and provides a simple callback-based API that can be
easily used from the mobile apps using native bridges (e.g. JNI).

* [quickjs-tart.git](https://git.taler.net/quickjs-tart.git)

The repository also includes qtart bindings for Android and iOS, which
handle the bridging so that the apps don't have to. The Android
bindings are available as a Maven library in Maven Central.

The main challenge for the Anastasis app was that Anastasis uses a
different API within wallet-core, that was originally not exposed to
qtart. This API is designed as a stateless reducer, that takes a state
and a transition and returns another state.

* [Reducer API docs](https://docs.anastasis.lu/reducer.html)

This is where things start to get interesting. In order to add
Anastasis support to qtart, I had to expose the Anastasis API in
wallet-core, implement a native QuickJS module for the Argon2 hash
function required by Anastasis, and ensure that the “anastasis-core”
subpackage compiles for QuickJS.

Once I had initial Anastasis support in qtart, the next step was to
add support for the API in the new Android app. I reused from the
wallet the code that manages the runtime and abstracts the API
operations as requests, but modified it to work with the stateless
model of the reducer. I also wrote Kotlin data structures and
serialization logic for all the JSON objects in the API.

At this point, I already had the starting point for the rest of the
app. I wrote the app using Jetpack Compose, a technology that I wasn't
very familiar with, so I had to learn it on the go, using the code of
other free-software apps I've used before as examples.

After some time, I managed to successfully create an abstraction for
the reducer on top of Compose, using a global view model injected to
the composables with Hilt. This view model contains the reducer state
as a StateFlow, and a ReducerManager object that allows the views to
safely interact with the API and update the state accordingly.

The routing part was easy, as the reducer state contains a property
that indicates the current step in the backup/recovery flow. The root
composable listens to this property and shows the screen that
corresponds to the backup/recovery step.

At some point when implementing the backup flow, I realized that I had
to use another (undocumented) API, separate to the reducer API, to
contact the backup providers and fetch the list of supported
verification methods. Again, I had to expose this API from
wallet-core, build quickjs-tart with this modification, and implement
this API on the app.

And, at some point when implementing the recovery flow, I learned
about YET another undocumented API, also separate to the reducer API,
that was needed to contact the backup providers and fetch the list of
secrets and recovery policies that match the identity attributes of
the user.

This resulted in another adventure, as this time, there was an issue
with data encoding and decoding in qtart related to some cryptographic
function that resulted in this API returning garbage. Florian helped
me fix this issue, and soon afterwards, the API was working just fine!

After some more work, I managed to have fully working backup and
recovery, with many missing features, of course, but with the basics
working just fine. The deadline was two days away, and I still had to
implement the Taler integration! Oh, no!

Well, unfortunately I couldn't get Taler integration working before
the deadline, but at least I got the backup fees showing in the UI,
and also added support for adding and removing backup providers, so
that users can choose where to backup their keys and learn about the
fees each provider demands beforehand. Isn't it cool?

# The current state

The Anastasis app, as it stands now, provides basic support for backup
and recovery of plain text keys (no files!). It allows users to manage
the backup providers and learn about the fees they demand, as well as
the fees per verification challenge (e.g. e-mail, SMS, question).

# What's left to do

There are many missing features:

* Some challenge types are not yet supported, such as IBAN, physical
  mail, and TOTP.
* It is only possible to backup plain-text secrets, as there's no
  functionality in the app to upload and download files.
* There's no support for paying backup providers with Taler.
* There's no mechanism to trigger a key backup from the wallet.
* The reducer state can't be saved or restored from a JSON file, nor
  is it stored in shared preferences.

I already have a working Anastasis instance that requires payments
with Taler, which I've been using to test Taler integration and
implement it in the app. This shouldn't take too long to fully
implement, but afterwards, better integration with the wallet would
still be needed to make the UX simpler.

Of course, a mechanism to trigger a backup or recovery from the wallet
is also missing. This will be implemented with an intent to the
Anastasis app with the data to backup or recover, and another intent
to the wallet app to hand-in the recovered data. Not too difficult!

# Merged code

All the required code in wallet-core and qtart has already been
merged. However, the latest wallet-core release (v0.9.3-dev.19, as of
now) still doesn't include them. The code for the Anastasis app,
present in the branch `dev/ivan-avalos/anastasis` of the taler-android
mono-repo, hasn't been merged yet, and it's still pending review from
the Taler developers.

* [taler-android.git](https://git.taler.net/taler-android.git)

# What I learned / challenges

One of the hardest parts, and the one that took me the longest, was
getting familiar with Jetpack Compose, especially creating an entire
app using only Compose. How would the architecture look like? How was
I supposed to translate the reducer model, not only to a screen, but
to an entire app? 

I had to look in a lot of places for inspiration, as I wanted
something clean, easy to work with, and above all, something that
actually made sense. I ended up modelling the app after the
architecture of the existing Anastasis web UI, written in React. As it
turned out, Compose is not too different from React!

Once I solved the initial challenges, I had a burst of creativity,
which helped me work fast on implementing most of the functionality of
the app. I refactored the app multiple times, and it always resulted
in a cleaner codebase and a better architecture. I did my best to have
a solid and sleek initial version, and so far it has paid off: less
headaches when working with my own code, and a modern and good-looking
app that I hope that will be a pleasure to use in the future.

During this Google Summer of Code, I learned to not be afraid of new
things, as in the end, there's always a way to figure them out. With
enough help from other people, and enough exploration, it's possible
for this process to be much shorter.
