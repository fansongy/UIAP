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
	
	[DllImport ("__Internal")]
	public static extern void pay(string order);

	[DllImport ("__Internal")]
	public static extern void getItems(string jsonList);

	static IAP s_instance = null;
	System.Action<string> m_curCallback  = null;
	
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
			if(Application.platform == RuntimePlatform.Android)
			{
				AndroidJavaClass jc = new AndroidJavaClass("com.uiap.MainActivity");
				AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
				jo.Call("pay",order);
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
				AndroidJavaClass jc = new AndroidJavaClass("com.uiap.MainActivity");
				AndroidJavaObject jo = jc.GetStatic<AndroidJavaObject>("currentActivity");
				jo.Call("getItems",jsonList);
			}
			else if(Application.platform == RuntimePlatform.IPhonePlayer)
			{
				getItems(jsonList);
			}

		}
	}
	
	public void onPay(string payData)
	{
		m_curCallback(payData);
	}

	public void onGetItem(string itemList)
	{
		m_curCallback(itemList);
	}
}
