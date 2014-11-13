package com.uiap;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;

import com.uiap.util.IabHelper;
import com.uiap.util.IabResult;
import com.uiap.util.Inventory;
import com.uiap.util.Purchase;
import com.uiap.util.SkuDetails;
import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

public class MainActivity extends UnityPlayerActivity {
	
		IabHelper mHelper;
		
		//Google Billing	
		String base64EncodedPublicKey;
		final String GOOGLE_TAG = "GoogleBilling";
		List<String> m_reqSkuList = new ArrayList<String>();
		
		public static MainActivity currentActivity = null;
		
		String mCurPurchase = "";
		 
		@Override protected void onCreate (Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			currentActivity = this;
			//Get ConfigData From Manifest
			ActivityInfo info = null;
			try {
				info = this.getPackageManager().getActivityInfo(getComponentName(),PackageManager.GET_META_DATA);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String key=info.metaData.getString("PublicKey");
			base64EncodedPublicKey = key; 
			
			String defaultSku=info.metaData.getString("defaultSku");
			String[] skus = defaultSku.split(",");
			m_reqSkuList = new ArrayList<String>();
			for(int i = 0 ;i<skus.length;++i)
			{
				m_reqSkuList.add(skus[i]);
			}

			initGoogleBill();
		}
		
		
		//------------Google Billing begin----------
		//init Google Billing and query unFinished order
		void initGoogleBill()
		{
			 // compute your public key and store it in base64EncodedPublicKey
			 mHelper = new IabHelper(this, base64EncodedPublicKey);
			 mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
				   public void onIabSetupFinished(IabResult result) {
						  if (!result.isSuccess()) {
						     // Oh noes, there was a probl em.
						     Log.d(GOOGLE_TAG, "Problem setting up In-app Billing: " + result);
						  }
						 // Hooray, IAB is fully set up!
						Log.d(GOOGLE_TAG, "Hooray, IAB is fully set up!");
						
						/*
						 *  Although we should call getList() when we init game, but If we are not, and call pay() directly,
						 * ,it will case an Error.so I add this to make sure it work.
						 * If you want control unfinished order checking by your self,
						 * you can delete under code,and choose right time to call getList()
						 */   
						mHelper.queryInventoryAsync(mQueryItems);
				   }
				});
		}
		
		
		final IabHelper.QueryInventoryFinishedListener 
		   mQueryItems = new IabHelper.QueryInventoryFinishedListener() {
		   public void onQueryInventoryFinished(IabResult result, Inventory inventory)   
		   {
		      if (result.isFailure()) {
		         // handle error
		         return;
		      }
		      
		      //put all items of inventory which is in skuList to JSONObject
		      JSONObject jsonResult = new JSONObject();
		      try
		      {     
			      for(int i = 0; i<m_reqSkuList.size();++i)
			      {
			    	  String sku = m_reqSkuList.get(i);
			    	  SkuDetails detail = inventory.getSkuDetails(sku);
			    	  JSONObject info = fillInfo(detail);
			    	  jsonResult.put(sku, info);

			      }
		      } catch (JSONException ex) {  
		    	  throw new RuntimeException(ex);  
		      }

		      String resultString  = jsonResult.toString();
		      Log.d(GOOGLE_TAG,"Inventory Result: "+ resultString);
		      
		      //responds the itemList data to Unity3d
		      if(jsonResult.length() >0) 
		      {
		    	  UnityPlayer.UnitySendMessage("Bridge", "onGetItem", jsonResult.toString());
		      }
		      
		      Log.d(GOOGLE_TAG, "check unfinished ...");
		      
		      //check unfinished purchase in skuList 
		      for(int i = 0; i<m_reqSkuList.size();++i)
		      {
		    	  String sku = m_reqSkuList.get(i);
		    	  Purchase pur = inventory.getPurchase(sku);
		    	  if (pur != null && verifyDeveloperPayload(pur)) {
	                  Log.d(GOOGLE_TAG, "find unfinished purchase, SKU: "+sku);
	                  mCurPurchase = sku;
	                  mHelper.consumeAsync(pur,mConsumeFinishedListener);
	                 return;
	              }
		      }

//		      pay("item1");
//           String jsonStr = "{\"items\":[\"item0\",\"item1\",\"item2\"]}";
//		      getList(jsonStr);
		   }
		};
		
		
		public void getList(String jsonStr)
		{		
			try
			{
				if(jsonStr.length() > 0)
				{
					JSONObject json = new JSONObject(jsonStr);
				  	JSONArray list = json.getJSONArray("items");
				  	m_reqSkuList = new ArrayList<String>();
				  	for(int i = 0 ;i<list.length();++i)
				  	{
				  		m_reqSkuList.add(list.getString(i));
				  	}
				}
				mHelper.queryInventoryAsync(true, m_reqSkuList,
						 mQueryItems);

			}
			 catch (JSONException ex) 
			 { 
				throw new RuntimeException(ex); 
			 }
			
		}
	
		public void pay(String itemSKU)
		{
			mCurPurchase = itemSKU;
			mHelper.launchPurchaseFlow(this, itemSKU, 10001, mPurchaseFinishedListener);
		}
		
		IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
	        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
	            Log.d(GOOGLE_TAG, "Purchase finished: " + result + ", purchase: "
	                    + purchase);
	            if (result.isFailure()) {
	            	Log.d(GOOGLE_TAG, "Error purchasing: " + result);
	                return;
	            }
	            if (!verifyDeveloperPayload(purchase)) {
	            	Log.d(GOOGLE_TAG, "Error purchasing. Authenticity verification failed.");
	                return;
	            }

	            Log.d(GOOGLE_TAG,  "Purchase successful.");

	            /*
	             * if your item is consume,you MUST call this.
	             * In Google Billing,everything isn't consumed until you call consumeAsync.
	             * Because nearly all IAP is consumed,I didn't make a data to save item kind 
	             */
	  	        if (purchase.getSku().equals(mCurPurchase)) {	  
		           // remove query inventory method from here and put consumeAsync() directly
		           mHelper.consumeAsync(purchase, mConsumeFinishedListener);
	  	        }

	        }
	    };

	    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener =
  		    new IabHelper.OnConsumeFinishedListener() {
  		    public void onConsumeFinished(Purchase purchase, IabResult result) {
  		        if (result.isSuccess()) {
  		           // provision the in-app purchase to the user
  		           // (for example, credit 50 gold coins to player's character)
  		           Log.d(GOOGLE_TAG,"Consume "+ mCurPurchase+" Success");
  		           
  		           //responds Unity3d purchase result
  		           UnityPlayer.UnitySendMessage("Bridge", "onPay", purchase.getOrderId());
  		           
  		       }
  		       else {
  		           // handle error
  		    	   Log.d(GOOGLE_TAG,"Consume "+ mCurPurchase+" Error");
  		      }
  		   }
  		};
	    
		JSONObject fillInfo(SkuDetails detail)
		{
			JSONObject data = null;
			try {
				if(detail != null)
				{
					  String price = detail.getPrice();
					  String desc = detail.getDescription();
					  String title = detail.getTitle();
					  String product = detail.getSku();
					  
					  data = new JSONObject();
					  
					  data.put("title", title);
					  data.put("price", price);
					  data.put("desc", desc);
					  data.put("product", product);
				}
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return data;
		}
		
	    @Override
	    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		    Log.d(GOOGLE_TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
	
		    // Pass on the activity result to the helper for handling
		    if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
		        // not handled, so handle it ourselves (here's where you'd
		        // perform any handling of activity results not related to in-app
		        // billing...
		        super.onActivityResult(requestCode, resultCode, data);
		    }
		    else {
		        Log.d(GOOGLE_TAG, "onActivityResult handled by IABUtil.");
		    }
		}
		
		
		  		
  		/** Verifies the developer payload of a purchase. */
	    boolean verifyDeveloperPayload(Purchase p) {
	        String payload = p.getDeveloperPayload();

	        /*
	         * TODO: verify that the developer payload of the purchase is correct. It will be
	         * the same one that you sent when initiating the purchase.
	         *
	         * WARNING: Locally generating a random string when starting a purchase and
	         * verifying it here might seem like a good approach, but this will fail in the
	         * case where the user purchases an item on one device and then uses your app on
	         * a different device, because on the other device you will not have access to the
	         * random string you originally generated.
	         *
	         * So a good developer payload has these characteristics:
	         *
	         * 1. If two different users purchase an item, the payload is different between them,
	         *    so that one user's purchase can't be replayed to another user.
	         *
	         * 2. The payload must be such that you can verify it even when the app wasn't the
	         *    one who initiated the purchase flow (so that items purchased by the user on
	         *    one device work on other devices owned by the user).
	         *
	         * Using your own server to store and verify developer payloads across app
	         * installations is recommended.
	         */

	        return true;
	    }
	 
}
