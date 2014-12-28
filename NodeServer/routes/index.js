var express = require('express');
var router = express.Router();
var shop = require('./shop');

/* GET home page. */
router.get('/', function(req, res) {
  res.render('index', { title: 'Express' });
});

router.post('/shop/verify',function(req,res){
	var reqBody = req.body;
	var type = reqBody.type; // 1 : App Store, 2 : Google Play
	var purchase = reqBody.purchase;
	var sig = reqBody.signature;
	var userId = reqBody.uid;

	shop.verify(userId, type, purchase, sig, function(err, data) {
		if(err)
		{
			res.status(400).json({
				result: err.status,
				msg:data
			})
		}
		else
		{
			res.status(200).json({
				result:data.status,
				msg:data.msg
			})
		}
		
	});
});

module.exports = router;
