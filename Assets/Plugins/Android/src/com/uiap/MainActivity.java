package com.uiap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.uiap.util.IabHelper;
import com.uiap.util.IabResult;
import com.uiap.util.Inventory;
import com.uiap.util.Purchase;
import com.uiap.util.SkuDetails;
import com.unity3d.player.UnityPlayerActivity;

public class MainActivity extends UnityPlayerActivity {
	
		IabHelper mHelper;
		
		//Google Billing	
		String base64EncodedPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAla7Z7tl7xGftCiJeA2rjJ1O/pfHjSb7mhHs64e0PTaPQcDsasIesf07iCSScACKtCHN8fzTW0t5eqjfgjKJkAkx6fRtizIyO09q9yDGwvQAj+lVbvjwRBG7kaI4moWMgwCFQQ6UNgjNrpO8KXojrkR3wjv72UeqIZ22F4U4+FBoIr+M2PqZ4utXpvUsh2H0zYsxj8uE771Xhemjk9jBFxnFMxWHAcSLS+rQatuS3/NohM4u2plo08xHP/nkSgLSmmBdnu4tQa4lzrsvzUI4qbFEgZbkCv+F2r4ml8Ot4NZjAvJ4JpOVj7bHhLU8Xkw3SfVGUCWOxn8iqyVpGrX50FQIDAQAB";
		final String GOOGLE_TAG = "GoogleTag";
		List<String> m_reqSkuList = Arrays.asList("item0","item1","item2");
		String mCurPurchase = "";
		 
		@Override protected void onCreate (Bundle savedInstanceState)
		{
			super.onCreate(savedInstanceState);
			initGoogleBill();
		}
		
		//------------Google Billing begin----------
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
						
						mHelper.queryInventoryAsync(mQueryItems);
				   }
				});
		}
		
		void pay(String itemSKU)
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
//	            if (!verifyDeveloperPayload(purchase)) {
//	                cLog.d(GOOGLE_TAG, "Error purchasing. Authenticity verification failed.");
//	                return;
//	            }

	            Log.d(GOOGLE_TAG,  "Purchase successful.");

	  	      if (purchase.getSku().equals(mCurPurchase)) {	  
	            // remove query inventory method from here and put consumeAsync() directly
	           mHelper.consumeAsync(purchase, mConsumeFinishedListener);

	  	      }

	        }
	    };
		
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
		
		IabHelper.OnConsumeFinishedListener mConsumeFinishedListener =
		  		   new IabHelper.OnConsumeFinishedListener() {
		  		   public void onConsumeFinished(Purchase purchase, IabResult result) {
		  		      if (result.isSuccess()) {
		  		         // provision the in-app purchase to the user
		  		         // (for example, credit 50 gold coins to player's character)
		  		    	  Log.d(GOOGLE_TAG,"Purchase "+ mCurPurchase+" Success");
		  		      }
		  		      else {
		  		         // handle error
		  		    	  Log.d(GOOGLE_TAG,"Purchase "+ mCurPurchase+" Error");
		  		      }
		  		   }
		  		};
		  		
		void getList(String jsonStr)
		{		
			try
			{
				JSONObject json = new JSONObject(jsonStr);
			  	JSONArray list = json.getJSONArray("items");
			  	m_reqSkuList = new ArrayList<String>();
			  	for(int i = 0 ;i<list.length();++i)
			  	{
			  		m_reqSkuList.add(list.getString(i));
			  	}
				 mHelper.queryInventoryAsync(true, m_reqSkuList,
						 mQueryItems);

			}
			 catch (JSONException ex) 
			 { 
				throw new RuntimeException(ex); 
			 }
			
		}

	  		
		final IabHelper.QueryInventoryFinishedListener 
		   mQueryItems = new IabHelper.QueryInventoryFinishedListener() {
		   public void onQueryInventoryFinished(IabResult result, Inventory inventory)   
		   {
		      if (result.isFailure()) {
		         // handle error
		         return;
		       }
		      
		      JSONObject jsonResult = new JSONObject();
		      try
		      {     
			      for(int i = 0; i<m_reqSkuList.size();++i)
			      {
			    	  String sku = m_reqSkuList.get(i);
			    	  SkuDetails detail = inventory.getSkuDetails(sku);
			    	  if(detail != null)
			    	  {
				    	  String price = detail.getPrice();
				    	  String desc = detail.getDescription();
				    	  String title = detail.getTitle();
				    	  String product = detail.getSku();
				    	  JSONObject data = new JSONObject();
				    	  data.put("title", title);
				    	  data.put("price", price);
				    	  data.put("desc", desc);
				    	  data.put("product", product);
				    	  jsonResult.put(sku, data);
			    	  }
			      }
		      } catch (JSONException ex) {  
		    	  throw new RuntimeException(ex);  
		      }
		      Log.d(GOOGLE_TAG, "check unfinished ...");
		      Log.d(GOOGLE_TAG,jsonResult.toString());
		      
		      //check SKU
		      for(int i = 0; i<m_reqSkuList.size();++i)
		      {
		    	  String sku = m_reqSkuList.get(i);
		    	  Purchase pur = inventory.getPurchase(sku);
		    	  if (pur != null){ // && verifyDeveloperPayload(pur)) {
	                  Log.d(GOOGLE_TAG, "find unfinished purchase, SKU:"+sku);
	                  mCurPurchase = sku;
	                  mHelper.consumeAsync(pur,
	                          mConsumeFinishedListener);
	                 return;
	              }
		      }

		      pay("item1");
//              String jsonStr = "{\"items\":[\"item0\",\"item1\",\"item2\"]}";
//		      getList(jsonStr);
		   }
		};
	 
}
