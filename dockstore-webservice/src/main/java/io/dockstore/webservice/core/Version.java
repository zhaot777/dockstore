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

package io.dockstore.webservice.core;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This describes one version of either a workflow or a tool.
 * 
 * @author dyuen
 */
@Entity
@ApiModel(value = "Base class for versions of entries in the Dockstore")
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public abstract class Version<T extends Version> implements Comparable<T>{
    /** re-use existing generator for backwards compatibility */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator="tag_id_seq")
    @SequenceGenerator(name="tag_id_seq", sequenceName="tag_id_seq")
    @ApiModelProperty("Implementation specific ID for the tag in this web service")
    private long id;

    @Column
    @JsonProperty("last_modified")
    @ApiModelProperty("The last time this image was modified in the image registry")
    private Date lastModified;

    @Column
    @ApiModelProperty(value = "git commit/tag/branch", required = true)
    private String reference;


    @OneToMany(fetch = FetchType.EAGER, orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinTable(name = "version_sourcefile", joinColumns = @JoinColumn(name = "versionid", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "sourcefileid", referencedColumnName = "id"))
    @ApiModelProperty("Cached files for each version. Includes Dockerfile and Descriptor files")
    @OrderBy("id")
    private final SortedSet<SourceFile> sourceFiles;

    public Version() {
        sourceFiles = new TreeSet<>();
    }

    @Column
    @ApiModelProperty("Implementation specific, whether this row is visible to other users aside from the owner")
    private boolean hidden;

    @Column
    @ApiModelProperty("Implementation specific, whether this tag has valid files from source code repo")
    private boolean valid;

    @Column
    @ApiModelProperty(value = "Implementation specific, can be a quay.io or docker hub tag name", required = true)
    private String name;

    public void updateByUser(final Version version) {
        reference = version.reference;
        hidden = version.hidden;
    }

    public void update(T version) {
        valid = version.isValid();
        lastModified = version.getLastModified();
        name = version.getName();
    }

    public void clone(T version) {
        name = version.getName();
        lastModified = version.getLastModified();
    }

    @JsonProperty
    public long getId() {
        return id;
    }

    @JsonProperty
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    @JsonProperty
    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Set<SourceFile> getSourceFiles() {
        return sourceFiles;
    }

    public void addSourceFile(SourceFile file) {
        sourceFiles.add(file);
    }


    @JsonProperty
    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    @JsonProperty
    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }



    @JsonProperty
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, lastModified, reference, hidden, valid, name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Version other = (Version) obj;
        return Objects.equals(this.id, other.id) && Objects.equals(this.lastModified, other.lastModified)
                && Objects.equals(this.reference, other.reference) && Objects.equals(this.hidden, other.hidden)
                && Objects.equals(this.valid, other.valid) && Objects.equals(this.name, other.name);
    }

    @Override
    public int compareTo(T that) {
        return ComparisonChain.start().compare(this.id, that.getId(), Ordering.natural().nullsLast())
                .compare(this.lastModified, that.getLastModified(), Ordering.natural().nullsLast())
                .compare(this.reference, that.getReference(), Ordering.natural().nullsLast())
                .compare(this.name, that.getName(), Ordering.natural().nullsLast()).result();
    }
}
