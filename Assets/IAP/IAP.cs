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
using System.Runtime.InteropServices;

public class IAP : MonoBehaviour {

#if UNITY_IOS
	[DllImport ("__Internal")]
	public static extern void pay(string order);
#endif

#if UNITY_ANDROID
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
			if(Application.platform == RuntimePlatform.Android)
			{
				requestPayLoad(order);
			}
			else if(Application.platform == RuntimePlatform.IPhonePlayer)
			{
				pay(order);
			}

		}
	}

	public  void getItemList(string jsonList,System.Action<string> callback)
	{
		if(Application.platform!= RuntimePlatform.OSXEditor)
		{
			m_curCallback = callback;
			Debug.Log("Call getItem");
			if(Application.platform == RuntimePlatform.Android)
			{
#if UNITY_ANDROID
				AndroidJavaClass jc = new AndroidJavaClass("com.uiap.MainActivity");
				AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
				jo.Call("getItems",jsonList);
#endif
			}
			else if(Application.platform == RuntimePlatform.IPhonePlayer)
			{
				getItems(jsonList);
			}

		}
	}

	public void onPay(string payData)
	{
		checkForServer(payData);
	}

	void checkForServer(string payData)
	{
		/*
		 * the payData is from the google server or Apple server 
		 *  we should use this data to check from our server.
		 */ 

		//As there is no server , I call the call back self... 
		Invoke("onFakeSCheckForServer",1);
	}

	void onFakeSCheckForServer()
	{
		onCheckForServer(0,"Purchase Succee");
	}

	void onCheckForServer(int nResult,string message)
	{
		m_order = "";
		if(nResult !=0)
		{
			Debug.LogError("check result is wrong");
			return;
		}
		m_curCallback(message);
	}

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
		Invoke("onFakeRequestPayLoad",1);
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
