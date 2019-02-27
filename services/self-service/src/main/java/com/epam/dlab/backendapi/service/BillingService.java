/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.BaseBillingDAO;
import com.epam.dlab.backendapi.resources.dto.BillingFilter;
import com.epam.dlab.backendapi.util.CSVFormatter;
import com.epam.dlab.exceptions.DlabException;
import jersey.repackaged.com.google.common.collect.Lists;
import org.bson.Document;

import java.text.ParseException;
import java.util.List;

public abstract class BillingService<T extends BillingFilter> {

	protected abstract Document getReport(UserInfo userInfo, T filter);

	protected String getValueOrEmpty(Document document, String key) {
		String value = document.getString(key);
		return value == null ? "" : value;
	}

	String getHeaders(boolean full) {
		return CSVFormatter.formatLine(getHeadersList(full), CSVFormatter.SEPARATOR);
	}

	public Document getBillingReport(UserInfo userInfo, T filter) {
		filter.getUser().replaceAll(s -> s.equalsIgnoreCase(BaseBillingDAO.SHARED_RESOURCE_NAME) ? null : s);
		return getReport(userInfo, filter);
	}

	public byte[] downloadReport(UserInfo userInfo, T filter) {
		return prepareReport(getReport(userInfo, filter)).getBytes();
	}

	String prepareReport(Document document) {
		try {
			StringBuilder builder =
					new StringBuilder(CSVFormatter.formatLine(Lists.newArrayList(getFirstLine(document)),
							CSVFormatter.SEPARATOR, '\"'));

			Boolean full = (Boolean) document.get(BaseBillingDAO.FULL_REPORT);
			builder.append(getHeaders(full));

			@SuppressWarnings("unchecked")
			List<Document> items = (List<Document>) document.get(BaseBillingDAO.ITEMS);

			items.forEach(d -> builder.append(getLine(full, d)));

			builder.append(getTotal(full, document));

			return builder.toString();
		} catch (ParseException e) {
			throw new DlabException("Cannot prepare CSV file", e);
		}
	}

	public abstract String getFirstLine(Document document) throws ParseException;

	public abstract List<String> getHeadersList(boolean full);

	public abstract String getLine(boolean full, Document document);

	public abstract String getTotal(boolean full, Document document);

	public abstract String getReportFileName(UserInfo userInfo, T filter);
}
