Tekdaqc-Android-Library
=======================

An Android library project wrapper of the [Tekdaqc Java Library](https://github.com/Tenkiv/Tekdaqc-Java-Library).

Join [Intelligent Automation, Computer Interface, & DAQ Community](https://plus.google.com/u/0/communities/109351353187504550254) on [![DAQ Community on Google Plus](https://ssl.gstatic.com/images/icons/gplus-16.png)](https://plus.google.com/u/0/communities/109351353187504550254) to stay up-to-date on the latest news.

The [Tekdaqc Android Manager](https://github.com/Tenkiv/Tekdaqc-Android-Manager) project is an open source, Android specific version of our [Tekdaqc Manager](https://github.com/Tenkiv/Tekdaqc-Manager), originally created by [Ian Thomas at ToxicBakery](https://github.com/ToxicBakery) as an example use of this library. [Check it out!](https://github.com/Tenkiv/Tekdaqc-Android-Manager)

## Using the Tekdaqc Android Library

### Dependencies
* [Tekdaqc Java Library](https://github.com/Tenkiv/Tekdaqc-Java-Library)

### Setup

Currently we only have Android Studio project files included with the repository. 

* If using gradle, add the following line to your `dependencies` section of your build script:
```gradle
 provided 'com.tenkiv.tekdaqc.android:android-library:1.0.0.1'
 ```
* The associated Maven descriptor is:
```xml 
<dependency>
  <groupId>com.tenkiv.tekdaqc.android</groupId>
  <artifactId>android-library</artifactId>
  <version>1.0.0.0</version>
  <type>aar</type>
</dependency>
```

Due to its dependency on the [Tekdaqc Java Library](https://github.com/Tenkiv/Tekdaqc-Java-Library), you will also need to add it as a dependency to your script:

* For gradle:
```gradle
 provided 'com.tenkiv.tekdaqc:java-library:1.0.0.0'
 ```

* For Maven
```xml
<dependency>
  <groupId>com.tenkiv.tekdaqc</groupId>
  <artifactId>java-library</artifactId>
  <version>1.0.0.0</version>
</dependency>
```

## More Information

### Other Tekdaqc GIT Repositories
* [Tekdaqc Java Library](https://github.com/Tenkiv/Tekdaqc-Java-Library)
* [Tekdaqc Firmware](https://github.com/Tenkiv/Tekdaqc-Firmware)

### Tekdaqc Manual
* Download the [Tekdaqc Manual here](http://www.tenkiv.com/tekdaqc_manual_pdf_v3.pdf)

### Other Links
* [Tenkiv Webpage](http://www.tenkiv.com/)
* [Intelligent Automation, Computer Interface, & DAQ Community](https://plus.google.com/u/0/communities/109351353187504550254) on [![DAQ Community on Google Plus](https://ssl.gstatic.com/images/icons/gplus-16.png)](https://plus.google.com/u/0/communities/109351353187504550254)

## Contributing

Please see our [contribution guidelines](https://github.com/Tenkiv/Tekdaqc-Android-Library/blob/master/CONTRIBUTING.md) if you have issues or code contributions.

### Contributors
#### Tenkiv, Inc.
* [Jared Woolston](https://github.com/jwoolston)

#### Third Party
* [Ian Thomas at ToxicBakery](https://github.com/ToxicBakery)

## License

    Copyright 2013 Tenkiv, Inc.
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
    http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
