/*
 * This file show how to use IAP Component
 * 
 * To use this component , it should be attached to camera
 * 
 */ 

using UnityEngine;
using System.Collections;

public class testPay : MonoBehaviour {

	string m_data;

	void OnGUI()
	{
		if(GUI.Button(new Rect(100,100,100,80),"ItemList"))
		{
			string list = "{\"items\":[\"item0\",\"item1\",\"item2\"]}";
			IAP.getInstance().getItemList(list,listBack);
		}
		if(GUI.Button(new Rect(400,100,100,80),"pay"))
		{
			IAP.getInstance().pay("item0",payBack);
		}
		GUI.Label(new Rect(100,400,800,800),m_data);

	}

	void payBack(string result)
	{
		m_data = result;
	}

	void listBack(string result)
	{
		m_data = result;
	}

}
