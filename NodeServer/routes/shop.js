
var crypto =require('crypto');

var logger = require('./logger');
var async = require('async');
var https = require('https');

// const PCN = "iapSample.ylyq.com";
const PCN = "com.ylyq.pandora";

var GOOGLE_PUBLIC_KEY_BASE64 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAla7Z7tl7xGftCiJeA2rjJ1O/pfHjSb7mhHs64e0PTaPQcDsasIesf07iCSScACKtCHN8fzTW0t5eqjfgjKJkAkx6fRtizIyO09q9yDGwvQAj+lVbvjwRBG7kaI4moWMgwCFQQ6UNgjNrpO8KXojrkR3wjv72UeqIZ22F4U4+FBoIr+M2PqZ4utXpvUsh2H0zYsxj8uE771Xhemjk9jBFxnFMxWHAcSLS+rQatuS3/NohM4u2plo08xHP/nkSgLSmmBdnu4tQa4lzrsvzUI4qbFEgZbkCv+F2r4ml8Ot4NZjAvJ4JpOVj7bHhLU8Xkw3SfVGUCWOxn8iqyVpGrX50FQIDAQAB";

var appleSandboxValidateHost = "sandbox.itunes.apple.com";
var appleValidateHost = "buy.itunes.apple.com";
var appleValidatePath = "/verifyReceipt";
var SandBoxVerifyErrorNo = 21007;

function verifyGoogleSig(body, sig) {
    var genPublicKey =function(key)
    {
        var chunkSize, chunks, str;
        str = key;
        chunks = [];
        chunkSize = 64;
        while (str) {
            if (str.length < chunkSize) {
                chunks.push(str);
                break;
            } else {
                chunks.push(str.substr(0, chunkSize));
                str = str.substr(chunkSize);
            }
        }
        str = chunks.join("\n");
        str = '-----BEGIN PUBLIC KEY-----\n' + str + '\n-----END PUBLIC KEY-----';
        return str;
    }
    var pubkey = genPublicKey(GOOGLE_PUBLIC_KEY_BASE64);
    var v = crypto.createVerify('RSA-SHA1').update(body);
    var res = v.verify(pubkey, sig, 'base64');
    return res;
}

function appleIapVerify(host, path, userId, purchaseId, body, callback) {
    var ret = {
        status : 0,
        msg:""
    };

    var httpsOptions = {
        hostname : host,
        path : path,
        method : "POST"
    }

    var reqApple = https.request(httpsOptions, function(resApple) {
        if (resApple.statusCode != 200) {
            logger.error(userId,"res Apple statusCode: ", resApple.statusCode);
            ret.status = -1;
            callback(ret);
        }
        var buf = "";
        resApple.on("data", function(data) {
            buf += data;
        });
        resApple.on("end", function() {
            logger.debug("apple Req end", buf);
            var status;
            var sku = -1;
            var bid = "";
            try {
                var resAppleObj = JSON.parse(buf);
                status = resAppleObj.status;
                if (status==0) {
                    sku = resAppleObj.receipt.product_id;
                    bid = resAppleObj.receipt.bid;
                    logger.debug("got status", status, "sku", sku,"bid",bid);
                }
            } catch(e) {
                logger.error(userId,"parse apple response err", e);
                ret.status = -2;
                callback(ret);
            }

            if (0 !== status) {
                if (status == SandBoxVerifyErrorNo) {
                    ret.status = SandBoxVerifyErrorNo;
                } else {
                    ret.status = 1;
                    logger.error(userId,"apple validate failed err", status, "fulltext", buf, ret.status);
                }
                callback(ret);
            } else if (bid != PCN) {
                //apple reciept other field check
                logger.error(userId,"apple validate failed err", status, "bid", bid);
                ret.status = 2;
                callback(ret);
            } else {       
                logger.info("App store verify ok user:", userId,"sku:", sku);

                //You should send product to user below 
                //The product info should config somewhere,and can be found by sku

                var coinOfSku = 1000;
                ret.msg = coinOfSku;
                callback(ret);
            }
        });
    });

    var httpTimeOut = 10000;
    reqApple.setTimeout(httpTimeOut, function() {
        reqApple.abort();
    });

    if (body) {
        // reqApple.write(JSON.stringify(body));
        reqApple.write(body);
    }

    reqApple.end();

    reqApple.on("error", function(e) {
        logger.error("reqApple err", e);
        ret.status = 3;
        callback(ret);
    });
}

exports.verify = function(userId, type, strInfo, strSig, callback) {
    if(type == 1) //App store
    {
        logger.debug(userId," App Store --- purchase info: ",strInfo);

        var msg  = JSON.stringify({"receipt-data":strInfo})
        async.waterfall([
            function(cb) {
                appleIapVerify(appleValidateHost, appleValidatePath, userId, strInfo, msg, function(data){
                    logger.debug(userId,"check App store",data);
                    cb(null,data);
                });
            },
            function(preCheck,cb) {
                // Still waitting for the app store check
                if (preCheck.status == SandBoxVerifyErrorNo ) {
                    appleIapVerify(appleSandboxValidateHost, appleValidatePath, userId, strInfo, msg, function(data){
                        if(data.status != 0)
                        {
                            logger.debug(userId,"check App store sandbox still error",data);
                            cb(data);
                        }
                        else
                        {
                           cb(null,data); 
                        }
                    });
                } else {
                    cb(preCheck);
                }
            }],
            function(err,result) {
                if (err) {
                   logger.error("verify final error",err);
                   callback(err);
                }
                else
                {
                    callback(null,result);
                }      
            }
        );
    }
    else if(type == 2) // google play
    {
        //parse payload info
        logger.debug(userId,'Google Play Verify --- signature is '+strSig+" info is:"+strInfo);
        
        var ret = {
            status : 0
        };

        //var rsaResult = verifyGoogleSig(GOOGLE_PUBLIC_KEY_BASE64, strInfo,strSig);
        var rsaResult = verifyGoogleSig(strInfo,strSig);
        if (true !== rsaResult) {
            logger.error(userId, "sig mismatch", strInfo, strSig);
            ret.status = -1;
            callback(ret,"sig mismatch");
            return;
        }

        //find amount in skus
        var purchaseInfo = null;
        try {
            purchaseInfo = JSON.parse(strInfo);
        } catch (e) {
            logger.error(e);
        }

        if (null == purchaseInfo) {
            logger.error(userId, "bad purchaseInfo", purchaseInfo);
            ret.status = 1;
            callback(ret,"bad purchaseInfo " + purchaseInfo);
            return;
        }

        if (0 !== purchaseInfo["purchaseState"]) {
            logger.error(userId, "bad purchase state", purchaseInfo);
            ret.status = 2;
            callback(ret,"google state not 0:" + purchaseInfo);
            return;
        }

        if (PCN != purchaseInfo["packageName"]) {
            logger.error(userId, "bad package name", purchaseInfo);
            ret.status = 3;
            callback(ret,"bad package name :" + purchaseInfo["packageName"]);
            return;
        }
        
        var sku = purchaseInfo['productId'];
        logger.debug("google verify ok ", userId, sku, amount);

        //You should send product to user below 
        //The product info should config somewhere,and can be found by sku

        var coinOfSku = 1000;
        callback(null,coinOfSku);
    }
    else //unknown platform
    {
        ret.status = -1;
        logger.error(userId, "google verify error no such platform: ",type);
        callback(ret);
    }
}
