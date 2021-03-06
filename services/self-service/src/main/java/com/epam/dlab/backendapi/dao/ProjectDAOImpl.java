/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.*;

public class ProjectDAOImpl extends BaseDAO implements ProjectDAO {

	private static final String PROJECTS_COLLECTION = "Projects";
	private static final String GROUPS = "groups";
	private static final String ENDPOINTS = "endpoints";
	private static final String STATUS_FIELD = "status";
	private static final String ENDPOINT_STATUS_FIELD = "endpoints." + STATUS_FIELD;
	private static final String EDGE_INFO_FIELD = "edgeInfo";
	private static final String ENDPOINT_FIELD = "endpoints.$.";
	private static final String ANYUSER = Pattern.quote("$anyuser");

	private final UserGroupDao userGroupDao;

	@Inject
	public ProjectDAOImpl(UserGroupDao userGroupDao) {
		this.userGroupDao = userGroupDao;
	}


	@Override
	public List<ProjectDTO> getProjects() {
		return find(PROJECTS_COLLECTION, ProjectDTO.class);
	}

	@Override
	public List<ProjectDTO> getProjectsWithStatus(ProjectDTO.Status status) {
		return find(PROJECTS_COLLECTION, eq(STATUS_FIELD, status.toString()), ProjectDTO.class);
	}

	@Override
	public List<ProjectDTO> getProjectsWithEndpointStatusNotIn(UserInstanceStatus... statuses) {
		final List<String> statusList =
				Arrays.stream(statuses).map(UserInstanceStatus::name).collect(Collectors.toList());

		return find(PROJECTS_COLLECTION, not(in(ENDPOINT_STATUS_FIELD, statusList)), ProjectDTO.class);
	}

	@Override
	public List<ProjectDTO> getUserProjects(UserInfo userInfo, boolean active) {
		final Set<String> groups = Stream.concat(userGroupDao.getUserGroups(userInfo.getName()).stream(),
				userInfo.getRoles().stream())
				.collect(Collectors.toSet());
		return find(PROJECTS_COLLECTION, userProjectCondition(groups, active), ProjectDTO.class);
	}

	@Override
	public void create(ProjectDTO projectDTO) {
		insertOne(PROJECTS_COLLECTION, projectDTO);
	}

	@Override
	public void updateStatus(String projectName, ProjectDTO.Status status) {
		updateOne(PROJECTS_COLLECTION, projectCondition(projectName),
				new Document(SET, new Document(STATUS_FIELD, status.toString())));
	}

	@Override
	public void updateEdgeStatus(String projectName, String endpoint, UserInstanceStatus status) {
		BasicDBObject dbObject = new BasicDBObject();
		dbObject.put(ENDPOINT_FIELD + STATUS_FIELD, status.name());
		updateOne(PROJECTS_COLLECTION, projectAndEndpointCondition(projectName,
				endpoint), new Document(SET, dbObject));
	}

	@Override
	public void updateEdgeInfo(String projectName, String endpointName, EdgeInfo edgeInfo) {
		BasicDBObject dbObject = new BasicDBObject();
		dbObject.put(ENDPOINT_FIELD + STATUS_FIELD, UserInstanceStatus.RUNNING.name());
		dbObject.put(ENDPOINT_FIELD + EDGE_INFO_FIELD, convertToBson(edgeInfo));
		updateOne(PROJECTS_COLLECTION, projectAndEndpointCondition(projectName, endpointName), new Document(SET,
				dbObject));
	}

	@Override
	public Optional<ProjectDTO> get(String name) {
		return findOne(PROJECTS_COLLECTION, projectCondition(name), ProjectDTO.class);
	}

	@Override
	public boolean update(ProjectDTO projectDTO) {
		BasicDBObject updateProject = new BasicDBObject();
		updateProject.put(GROUPS, projectDTO.getGroups());
		updateProject.put(ENDPOINTS,
				projectDTO.getEndpoints().stream().map(this::convertToBson).collect(Collectors.toList()));
		return updateOne(PROJECTS_COLLECTION, projectCondition(projectDTO.getName()),
				new Document(SET, updateProject)).getMatchedCount() > 0L;
	}

	@Override
	public void remove(String name) {
		deleteOne(PROJECTS_COLLECTION, projectCondition(name));
	}

	@Override
	public Optional<Integer> getAllowedBudget(String project) {
		return get(project).map(ProjectDTO::getBudget);
	}

	@Override
	public void updateBudget(String project, Integer budget) {
		updateOne(PROJECTS_COLLECTION, projectCondition(project), new Document(SET, new Document("budget", budget)));
	}

	@Override
	public boolean isAnyProjectAssigned(Set<String> groups) {
		final String groupsRegex = !groups.isEmpty() ? String.join("|", groups) + "|" + ANYUSER : ANYUSER;
		return !Iterables.isEmpty(find(PROJECTS_COLLECTION, elemMatch(GROUPS, regexCaseInsensitive(groupsRegex))));
	}

	private Bson projectCondition(String name) {
		return eq("name", name);
	}

	private Bson userProjectCondition(Set<String> groups, boolean active) {
		final String groupsRegex = !groups.isEmpty() ? String.join("|", groups) + "|" + ANYUSER : ANYUSER;
		if (active) {
			return and(elemMatch(GROUPS, regexCaseInsensitive(groupsRegex)),
					eq(ENDPOINT_STATUS_FIELD, UserInstanceStatus.RUNNING.name()));
		}
		return elemMatch(GROUPS, regexCaseInsensitive(groupsRegex));
	}

	private Bson projectAndEndpointCondition(String projectName, String endpointName) {
		return and(eq("name", projectName), eq("endpoints.name", endpointName));
	}

	private Document regexCaseInsensitive(String values) {
		return new Document("$regex",
				"^(" + values + ")$").append("$options", "i");
	}
}
