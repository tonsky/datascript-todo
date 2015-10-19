# DataScript ToDo Sample Application

## Development Build

```
lein cljsbuild auto none &
open index.html
```

## For LightTable UI

For inline evaluation of clojurescript within LightTable you must manually
update LightTable's Clojure plugin to 0.2.0 or later.  As of this writing,
the plugin version is 0.1.0 and does not update unless you delete the plugin
and re-install it from the LightTable GUI.

Invoke the build at least once before evaluating anything in LightTable, by
either following the Development Build instructions above to build and 
automatically watch for changes or build just once by using:

```
lein clsjbuild once none
```

It's important to use `none` to invoke the correct cljsbuild identifier,
otherwise advanced optimizations will prevent LightTable from finding `goog`
