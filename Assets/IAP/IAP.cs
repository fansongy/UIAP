/*
 * This class is designed to add In-App Purchaes
 * 
 * Currently,there are two interface which use as follow:
 * 
 * Usage:
 * 
 * 	1.pay
 *  You can use pay(orderName,callBackFunc) to make it work.
 *  callBackFunc has a string parameter and return void.
 * 
 *  2.getItem
 * 
 *  You can use getItem(jsonList) to get the ItemList of IAP.
 *  The json format should be like:
 *  {
 * 		"items": ["item0","item1","item2"]
 *  }
 *  callBackFunc has a string parameter and return void. The parameter is also a json which include the all info of items.
 * 
 *  On ios part, there is several file which is worte by native language.
 *  They are in Plugins/IOS. After export the xcode project,they will be copyed to Library/ folder.
 *  Before you connect you deivce to debug, makesure your xcode team and Bundle Identifier is correct. You also need to add StoreKit.framewrok
 * 		
 * 
 */ 

using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using System.Runtime.InteropServices;

public class IAP : MonoBehaviour {

#if UNITY_IOS
	[DllImport ("__Internal")]
	public static extern void pay(string order); 
#elif UNITY_ANDROID
	[DllImport ("__Internal")]
	public static extern void payWithPayLoad(string order,string payLoad);
#endif

	[DllImport ("__Internal")]
	public static extern void getItems(string jsonList);

	static IAP s_instance = null;
	System.Action<string> m_curCallback  = null;

	string m_order;

	public static IAP getInstance()
	{
		if(s_instance == null)
		{
			GameObject obj = new GameObject();
			obj.name = "Bridge";
			s_instance = obj.AddComponent<IAP>();
		}
		return s_instance;
	}

	public  void pay(string order,System.Action<string> callback)
	{

		if(Application.platform!= RuntimePlatform.OSXEditor)
		{
			Debug.Log("Call Pay on Android platform");
			m_curCallback = callback;
			m_order = order;

			#if UNITY_ANDROID
				requestPayLoad(order);
			#elif UNITY_IOS
				pay(order);
			#endif
		}
	}

	public  void getItemList(string jsonList,System.Action<string> callback)
	{
		if(Application.platform!= RuntimePlatform.OSXEditor)
		{
			m_curCallback = callback;
			Debug.Log("Call getItem");
			#if UNITY_ANDROID
				AndroidJavaClass jc = new AndroidJavaClass("com.uiap.MainActivity");
				AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
				jo.Call("getItems",jsonList);
			#elif UNITY_IOS
				getItems(jsonList);
			#endif
		}
	}

	public  void onPay(string payData)
	{
		Debug.Log("Recv on Pay:"+payData);
		m_curCallback(payData);
	}

//	IEnumerator checkForServer(string payData)
//	{
//		/*
//		 * the payData is from the google server or Apple server 
//		 *  we should use this data to check from our server.
//		 */ 
//
//		//As there is no server , I call the call back self... 
////		payData = "{\"purchase\":\"{\"orderId\":\"12999763169054705758.1304973360679858\",\"packageName\":\"iapSample.ylyq.com\",\"productId\":\"coin1\",\"purchaseTime\":1418462802885,\"purchaseState\":0,\"developerPayload\":\"asdffdsa\",\"purchaseToken\":\"flbagkkkmklolhpbanamfonl.AO-J1OyMCWN3X9PNxHRic59kMo-6QxmKRXT5QeTgmKMBYAFcwAShvWm5CuoNzShzOKPPwuFXySRH4ps0mtWwuEhSBI92a6GbbRyiEiLLbz8wCRXtfTwBZB0\"}\",\"signature\":\"WmAcC4fhPlPCoO1pgnlRflxqVq5Zv\\/sWRmHajwgSW1MzJ69Kqk4Hx\\/Pv89D+seRgjZUjlEgowchMphC1qOUAJ4tKeLKNc232aKehslFTD6QKANKxFvFoNI3\\/Iqd1VdkdW9wAEZvLH8en2GGfuAs3vs7Y+sKPLFQySd0fRSjqiNpQf8udH537BjwVm9+OOZR0wsBAH9l17eNIsCJ2sraMj4m+4BRvlHm\\/OHTGehSWmkI\\/17rlWEJ9sPrG5BJMvXyF9qubuqzh8OEh0\\/teO9FGa+i1AksfrmmpN2je\\/sh33Y6oQyaQpG7GKEKVbRxUsfjvcABTENBwG4nYp2DdpHLELQ==\"}";
//
//		var json = MiniJSON.Json.Deserialize(payData) as Dictionary<string,System.Object>;
//		string sign = json["signature"] as string;
//		string purchase = json["purchase"] as string;
//
//		WWWForm form = new WWWForm();
//		form.AddField("signature",sign);
//		form.AddField("purchase",purchase);
//		form.AddField("type","2");
//		form.AddField("uid", DataStoreAgent.Instance().Uid);
//		
//		var upload = new WWW(URLs.URL_PREFIX+URLs.PURCHASE_VERIFY_URL,form);
//		yield return upload;
//		if(!string.IsNullOrEmpty(upload.error)) {
//			Debug.LogError( "Error downloading: " + upload.error );
//			onCheckForServer(null);
//		} else {
//			// show the highscores
//			Debug.Log(upload.text);
//			onCheckForServer(upload.text);
//		}
////		Invoke("onFakeSCheckForServer",1);
//	}

//	void onFakeSCheckForServer()
//	{
//		onCheckForServer("Purchase Succee");
//	}

//	void onCheckForServer(string message)
//	{
//		Debug.Log("onCheckForServer messge is: "+message);
//		int nResult = 0;
//		string msg ;
//		object obj ;
//		WebServiceClient.parseAPIResult(message,out nResult,out msg,out obj);
//
//		Debug.Log("parse Result is : nResult = "+nResult+" msg="+msg+" obj"+obj.ToString());
//
//		if(nResult !=0)
//		{
//			Debug.LogError("check result is wrong:"+msg);
//			m_intCurCallback(-1);
//		}
//		else
//		{
//			System.Int64 resInt;
//			if (!System.Int64.TryParse(obj.ToString(), out resInt)) {
//				Debug.LogError("parse chip failed");
//				m_intCurCallback(-1);
//				return;
//			}
//			m_intCurCallback(resInt);
//		}
//	}
//
	public void onGetItem(string itemList)
	{
		m_curCallback(itemList);
	}

	void requestPayLoad(string order)
	{
		/*
		 * we should connect to our verify server in this function
		 * the parameter server needed may be device id , user name ,order,platform and so on
		 * server will make a payload by some kind of algorithm 
		 * then it will call our call back function.
		 */ 

		//As there is no server , I call the call back self... 

		Invoke("onFakeRequestPayLoad",0.1f);
	}
				
	//this function is used to simulate call back from server
	void onFakeRequestPayLoad()
	{
		onRequestPayLoad(0,"asdffdsa");
	}

	public void onRequestPayLoad(int nResult,string payload)
	{
		if(nResult != 0) //get payload error
		{
			Debug.LogError("get pay load error, msg from server is "+payload);
			m_order = "";
			return;
		}
#if UNITY_ANDROID
		AndroidJavaClass jc = new AndroidJavaClass("com.uiap.MainActivity");
		AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
		jo.Call("payWithPayLoad",m_order,payload);
#endif
	}
}
