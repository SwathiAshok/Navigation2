package com.example.swathi.navigation;

import android.net.wifi.ScanResult;

import java.util.Comparator;

public class AccessPointComparer implements Comparator<ScanResult> {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public int compare(ScanResult lhs, ScanResult rhs) {
		 return lhs.level < rhs.level ? 1 : lhs.level > rhs.level ? -1 : 0;
	}

}
