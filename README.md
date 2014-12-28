UIAP
===========

## What is this? ##
After we developing the game by Unity3d, we need to add purchase model to make money. 

This project is designed to make it add In-App-Purchase easy. Currently support:

* App Store 
* Google Billing 

As a solution, UIAP include client and server. The server is based on Express of Node.js. The server code is used to verify purchase,you can run it in your host. 

## How to install? ##

### Client ###
You can download [this package](https://www.dropbox.com/s/w4a3pt64a49you9/UIAP_v1.1.unitypackage?dl=0)(i can't visit that url,[try this](http://pan.baidu.com/s/1eQs1Kbw)) and import to Unity3d .

Or you can clone this repo, copy `IAP` folder to `Client/Assets/`. you should also copy the file under the `Plugins` to your `Assets/Plugins`.

There's a demo project in `IAP/Example`,which show how to use the interface.

### Server ###
You can copy all file in `NodeServer` to your host, and type under code:

	node bin/www
Doing that will run the express. Anyway, you may need to change the server ip which client connect, it should be configed in `IAP.cs`.

## How to Use? ##
After installing the plugin,we can use it by following code:

### 1. getItems: get the information of the goods ###

    string list = "{\"items\":[\"item0\",\"item1\",\"item2\"]}";
    IAP.getInstance().getItemList(list,listBack);

The first parameter is the skulist. Considering the number of the goods could increase, when we want to get the item info,we should offer the sku name.

It should follow the json format,such as:

    {
        "items":
        [
            "item0",
            "item1",
            "item2"
        ]
    } 

Second parameter is the callback,It should look like:

    void listBack(string result)
    {
        //do something        
    }

Parameter of this function also follow json format.

### 2. pay: purchase the goods ###

    IAP.getInstance().pay("item0",payBack);

The first parameter is sku of items. The second is callback which looks like:

    void payBack(string result)
    {
        //do something
    }


## Export and Package ##
After adding the interface, we should config the export settings.

### App Store ###
In order to add IAP into an App Store game,we should follow standard operation to export.

1. Change the Bundle Identifier of your app.
2. Keep your code sign of Xcode is correct.
3. Add StoreKit.framework in Xcode Project.
4. Connect your device and build the package.

### Google Billing ###
In order to add Google Billing into your game,we should follow standard operation to export.

1. open AndroidManifest.xml in Assets/Plugins/Android. Change your public key of google billing. 
2. you should also edit the defaultSku under the public key, which is used to update status when the app init.
3. Change the Bundle Identifier of your app.
4. Set your KeyStore.
5. Export the package.

## Support Version ##

* Unity3d v4+
* IOS 6.0+
* Google Billing v3

## Limitations and Future ##

Currently the info which is passed between native code and Unity3d is hard Code,if you want change it you should change the file inside.


I may work on those in the future.

## License ##

UIAP is published under MIT License

    Copyright (c) 2014 SongYang Fan (@fansongy)
    
    Permission is hereby granted, free of charge, to any person obtaining a copy of
    this software and associated documentation files (the "Software"), to deal in
    the Software without restriction, including without limitation the rights to use,
    copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
    Software, and to permit persons to whom the Software is furnished to do so,
    subject to the following conditions:
    
    The above copyright notice and this permission notice shall be included in all
    copies or substantial portions of the Software.
    
    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
    FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
    COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
    IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
    CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

