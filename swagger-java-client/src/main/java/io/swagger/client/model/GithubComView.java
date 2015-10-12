package io.swagger.client.model;

import io.swagger.client.StringUtil;
import io.swagger.client.model.GitHubComAuthenticationResource;



import io.swagger.annotations.*;
import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
@javax.annotation.Generated(value = "class io.swagger.codegen.languages.JavaClientCodegen", date = "2015-10-05T12:31:03.778-04:00")
public class GithubComView   {
  
  private GitHubComAuthenticationResource parent = null;

  
  /**
   **/
  @ApiModelProperty(value = "")
  @JsonProperty("parent")
  public GitHubComAuthenticationResource getParent() {
    return parent;
  }
  public void setParent(GitHubComAuthenticationResource parent) {
    this.parent = parent;
  }

  

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class GithubComView {\n");
    
    sb.append("    parent: ").append(StringUtil.toIndentedString(parent)).append("\n");
    sb.append("}");
    return sb.toString();
  }
}