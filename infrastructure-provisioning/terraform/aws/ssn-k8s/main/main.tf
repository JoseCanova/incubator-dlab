# *****************************************************************************
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
# ******************************************************************************

provider "aws" {
  region     = var.region
  access_key = var.access_key_id
  secret_key = var.secret_access_key
}

output "ssn_k8s_alb_dns_name" {
  value = aws_lb.ssn_k8s_alb.dns_name
}

output "ssn_k8s_masters_ip_addresses" {
  value = data.aws_instances.ssn_k8s_masters_instances.public_ips
  depends_on = [data.aws_instances.ssn_k8s_masters_instances]
}

output "ssn_bucket_name" {
  value = aws_s3_bucket.ssn_k8s_bucket.id
}

output "ssn_vpc_id" {
  value = data.aws_vpc.ssn_k8s_vpc_data.id
}

output "ssn_subnets" {
  value = compact([data.aws_subnet.k8s-subnet-a-data.id, data.aws_subnet.k8s-subnet-b-data.id, local.subnet_c_id])
}

output "ssn_k8s_sg_id" {
  value = aws_security_group.ssn_k8s_sg.id
}