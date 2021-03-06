/*
 *    Copyright 2016 OICR
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package io.swagger.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaJerseyServerCodegen", date = "2016-09-12T21:34:41.980Z") public abstract class ToolsApiService {
    public abstract Response toolsGet(String id, String registry, String organization, String name, String toolname, String description,
            String author, String offset, Integer limit, SecurityContext securityContext) throws NotFoundException;

    public abstract Response toolsIdGet(String id, SecurityContext securityContext) throws NotFoundException;

    public abstract Response toolsIdVersionsGet(String id, SecurityContext securityContext) throws NotFoundException;

    public abstract Response toolsIdVersionsVersionIdDockerfileGet(String id, String versionId, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response toolsIdVersionsVersionIdGet(String id, String versionId, SecurityContext securityContext)
            throws NotFoundException;

    public abstract Response toolsIdVersionsVersionIdTypeDescriptorGet(String type, String id, String versionId,
            SecurityContext securityContext) throws NotFoundException;

    public abstract Response toolsIdVersionsVersionIdTypeDescriptorRelativePathGet(String type, String id, String versionId,
            String relativePath, SecurityContext securityContext) throws NotFoundException;

    public abstract Response toolsIdVersionsVersionIdTypeTestsGet(String type, String id, String versionId, SecurityContext securityContext)
            throws NotFoundException;
}
