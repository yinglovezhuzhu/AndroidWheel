/*
 * Copyright (C) 2014. The Android Open Source Project.
 *
 *         yinglovezhuzhu@gmail.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.opensource.wheel.demo.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yinglovezhuzhu@gmail.com
 *
 */
public class CityResp extends BaseListResp<CityResp.City> {
	
	
	/**
	 * 城市数据类型
	 * @author xiaoying
	 *
	 */
	public static class City {
		private int id;
		private int parentId;
		private String name;
		private List<City> childCity = new ArrayList<City>();
		/**
		 * @return the id
		 */
		public int getId() {
			return id;
		}
		/**
		 * @param id the id to set
		 */
		public void setId(int id) {
			this.id = id;
		}
		/**
		 * @return the parentId
		 */
		public int getParentId() {
			return parentId;
		}
		/**
		 * @param parentId the parentId to set
		 */
		public void setParentId(int parentId) {
			this.parentId = parentId;
		}
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}
		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		/**
		 * @return the childCity
		 */
		public List<City> getChildCity() {
			return childCity;
		}
		/**
		 * @param childCity the childCity to set
		 */
		public void setChildCity(List<City> childCity) {
			this.childCity = childCity;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return "City [id=" + id + ", parentId=" + parentId + ", name="
					+ name + ", childCity=" + childCity + "]";
		}
	}
}
