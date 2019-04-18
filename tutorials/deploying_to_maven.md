
#How _(not)_ to deploy to Maven Central with sbt

This AES implementation in Chisel is now available on [Maven Central](https://search.maven.org/search?q=g:com.github.hplp),
so anyone can easily import it into their projects. You should deploy your awesome library too! Here's how deploying went for me.

## Use this guide and you'll be done in no time!

According to Chick and Jim, using this [in-depth-guide-to-deploying-to-maven-central](https://leonard.io/blog/2017/01/an-in-depth-guide-to-deploying-to-maven-central/)
should make it all trivial. And it was indeed very helpful and easy - I highly recommend following these steps.

Even so, I made a few mistakes, got stuck, searched online for help, and eventually figured it out.

## A few additional details

* If, after adding the `sbt-php` plugin in _project/plugins.sbt_ you still get an error when you run `sbt php-cmd`, 
try running `sbt clean` followed by `sbt compile`.
  * While in the `sbt` shell, I found it helpful to test other `pgp-cmd` commands such as `list-keys`

* Settings in _build.sbt_:
  * Did you also start with the [chisel template](https://github.com/freechipsproject/chisel-template)? I certainly did,
  and only updated the `name` value. Do yourself a favour, don't repeat my mistake and update the `version` value to something
  reasonable. This value will appear on Maven.
  * If your code is on GitHub under _github.com/username/reponame_ - it seems to be recommended to set the `organization` 
  to `"com.github.username"`
  * You can also set the `organizationName` and `homepage` parameters
  * Also, to make your life easy when publishing and updating, use these 2 lines

```sbtshell
publishConfiguration := publishConfiguration.value.withOverwrite(true)
publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
```

* After generating a PGP key and deploying it to a key server, have some patience - the key will not be available on all servers immediately.
  * In the _sbt_ shell, start by typing `pgp-cmd` and press _Tab_ to see the available options.
* I also consulted the [sbt Sonatype setup official guide](http://www.scala-sbt.org/release/docs/Using-Sonatype.html) to clarify a few things here and there.
