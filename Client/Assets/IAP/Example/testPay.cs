/*
 * This file show how to use IAP Component
 * 
 * To use this component , it should be attached to camera
 * 
 * version: 1.1
 * 
 * Author: SongYang Fan 
 *
 * Project: https://github.com/fansongy/UIAP   
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

	void payBack(System.Int64 result)
	{
		if(result == -1)
		{
			m_data = "purchase fail";
		}
		else
		{
			m_data = "User get "+result+" coins";
		}
	}

	void listBack(string result)
	{
		m_data = result;
	}

}
