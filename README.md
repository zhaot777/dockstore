[![Build Status](https://travis-ci.org/CancerCollaboratory/dockstore-descriptor.svg)](https://travis-ci.org/CancerCollaboratory/dockstore-descriptor)
[![Coverage Status](https://coveralls.io/repos/CancerCollaboratory/dockstore-descriptor/badge.svg?branch=develop)](https://coveralls.io/github/CancerCollaboratory/dockstore-descriptor?branch=develop)

# Dockstore Descriptor

The purpose of this tool is to show how [CWL](http://ga4gh.org/#/cwf-team) can be used to describe and run a tool inside a Docker container.

This repo contains an example HelloWorld workflow that simply generates a "helloworld" text file and sticks it in an output location on the filesystem, an example Descriptor for how to run this Docker-ized HelloWorld workflow (in JSON and CWL), and a sample Java Launcher that interprets the Descriptor, provisions files, and launches the constructed command.  The goal of this repo is to provide a concrete example of this working so I can get feedback from the community on the goal of using CWL with Docker.  This prototype can also be adapted into [Consonance](https://github.com/Consonance/), our cloud orchestration framework, for real production use. Finally, it's useful for developers to use as a testing tool with their Dockers described using CWL. 

## Dependencies

You need the following on your system to use the launcher:

* java 1.8
* maven
* docker
* the cwl command line utils (see below)

For the last item you need to do something like the following:

```
$ pip install cwl-runner
```

## The Launcher

The Launcher Java program is just a proof of concept.  The code will eventually be folded into the Consonance worker daemon which needs to do the items below in addition to interacting with the consonance queue to pull orders.  This launcher below is a simplification and just focuses on constructing a command and dealing with inputs/outputs as a prototype.

0. pulls over config file to `~/.consonance/launcher.config`, uses same mechanism as other config files but needs to be done first, see below. For this demo it assumes the config file is provided on the command line.
0. makes a working directory in `/datastore/launcher-<uuid>` (from the config file above, this demo assumes `/datastore` is the big disk to use here but it could be any path)
0. pulls over 0 or more files that were associated with this workflow order to `/datastore/launcher-<uuid>/configs`, these will be used by the workflow itself. These will come from a web service endpoint in Consonance rather than external sources like inputs below. This is how we get a SeqWare INI file for example. For this demo launcher this functionality will be skipped since it lacks a queue/web service to talk to. 
0. make `/datastore/launcher-<uuid>/working` to be used as the working directory for the command, `/datastore/launcher-<uuid>/inputs` for all the file inputs, and `/datastore/launcher-<uuid>/logs` for logs
0. pull down all the Docker images referenced in the descriptor
0. start services referenced in the descriptor, this functionality does not yet exist
0. download all the inputs, these will come from S3, HTTP/S, SFTP, FTP, ICGCObjectStore, etc, put them in locations within `/datastore/launcher-<uuid>/inputs`
0. construct the command, this includes `-v` for all config and input files, `-v` for the working directory /datastore/launcher-<uuid>/working, the `docker run <image_id:version>` parts of the command along with the actual command being run.
0. run the command, noting success/failure, stderr/stdout going to `/datastore/launcher-<uuid>/logs`
0. collect and provision output files to their destination referenced in `~/.consonance/launcher.config`

### The Config file

```
working-directory=./datastore/
```

### Resulting Docker Command

The command is constructed for this HelloWorld tool:

    docker run -v $ref_file_1:$ref_file_1.destination -v $ref_file_2:$ref_file_2.destination dockerId '$cmd'

So the command is from the JSON, in this case:

    cat $hello-input > $hello-output && ls $ref_file_2 >> $hello-output && head -20 $ref_file_2 >> $hello-output

And the various `$` field are filled in for local values within the docker container and executed with the CWD being `/datastore/launcher-<uuid>/working`.

### Building

Standard maven build in the launcher directory. Notice I'm unsetting my AWS credentials if I have them set already since the tests expect a credentials failure:

    unset AWS_ACCESS_KEY
    unset AWS_SECRET_KEY
    mvn clean install
    # to skip tests
    mvn clean install -DskipTests

### Running the Launcher

#### JSON Parameters

If you do not have access to the OICR AWS account you will want to change the output URL to a location you can write to in S3:

```
collab-cwl-job-pre.json
```

#### Running

To run the Launcher:

    java -jar <launcher.jar> --config <path_to_launcher.config> --decriptor <path_to_json_descriptor>
    # for example:
    java -jar launcher/target/uber-io.github.collaboratory.launcher-1.0.0.jar --config launcher.ini --descriptor collab.json
    # another example for testing
    rm -rf datastore && cd launcher && mvn clean install && cd - && java -jar launcher/target/uber-io.github.collaboratory.launcher-1.0.0.jar --config launcher.ini --descriptor collab.json

The above will fail with an `AmazonS3Exception: Access Denied` since the collab.json points to an output location on S3 (`s3://oicr.temp/testing-launcher` specifically).  So you need to include your Amazon keys as env vars to be picked up by the API we're using to do uploads.  The correct command would be (of course fill in your own values here):

    export AWS_ACCESS_KEY=AAAAAAA
    export AWS_SECRET_KEY=SSSSSSS
    rm -rf datastore && cd launcher && mvn clean install && cd - && java -jar launcher/target/uber-io.github.collaboratory.launcher-1.0.0.jar --config launcher.ini --descriptor collab.json

If you change the `collab.json` to point to other destinations (like SFTP) you will need to pass in auth params in a similar way, see the [VFS Docs](http://commons.apache.org/proper/commons-vfs/api.html).

## The CWL-Based Launcher

The CWL Launcher Java program is just a proof of concept.  The code will eventually be folded into the Consonance worker daemon which needs to do the items below in addition to interacting with the consonance queue to pull orders.  This launcher below is a simplification and just focuses on constructing a command and dealing with inputs/outputs as a prototype.

The difference between this one and the regular Launcher is the nature of the descriptor files used.  Instead of using a JSON descriptor it takes a CWL-based descriptor along with a JSON file that specifies parameters to the tool.  The config file is unchanged and the pattern is very similar to the JSON-based Launcher above.

0. pulls over config file to `~/.consonance/launcher.config`, uses same mechanism as other config files but needs to be done first, see below. For this demo it assumes the config file is provided on the command line.
0. makes a working directory in `/datastore/launcher-<uuid>` (from the config file above, this demo assumes `/datastore` is the big disk to use here but it could be any path)
0. pulls over 0 or more files that were associated with this workflow order to `/datastore/launcher-<uuid>/configs`, these will be used by the workflow itself. These will come from a web service endpoint in Consonance rather than external sources like inputs below. This is how we get a SeqWare INI file for example. For this demo launcher this functionality will be skipped since it lacks a queue/web service to talk to. 
0. make `/datastore/launcher-<uuid>/working` to be used as the working directory for the command, `/datastore/launcher-<uuid>/inputs` for all the file inputs, and `/datastore/launcher-<uuid>/logs` for logs
0. start services referenced in the descriptor, this functionality does not yet exist
0. download all the inputs, these will come from S3, HTTP/S, SFTP, FTP, ICGCObjectStore, etc, put them in locations within `/datastore/launcher-<uuid>/inputs`
0. produces a new JSON parameterization document that has the URLs replaced with file paths that are local to the container host
0. hands the updated JSON parameterization document and CWL descriptor to the CWL runner tool, this causes the command to be constructed, docker containers to be pulled and the command to be run correctly
0. collect and provision output files to their destination referenced in `~/.consonance/launcher.config`

### Running the CWL-Based Launcher

#### JSON Parameters

If you do not have access to the OICR AWS account you will want to change the output URL to a location you can write to in S3:

```
collab-cwl-job-pre.json
```

#### Running

To run the Launcher:

    export AWS_ACCESS_KEY=AAAAAAA
    export AWS_SECRET_KEY=SSSSSSS
    java -cp <launcher.jar> io.github.collaboratory.LauncherCWL --config <path_to_launcher.config> --decriptor <path_to_json_descriptor>
    # for example:
    java -cp launcher/target/uber-io.github.collaboratory.launcher-1.0.0.jar io.github.collaboratory.LauncherCWL --config launcher.ini --descriptor collab.cwl --job collab-cwl-job-pre.json
    # another example for testing
    rm -rf datastore && cd launcher && mvn clean install && cd - && java -cp launcher/target/uber-io.github.collaboratory.launcher-1.0.0.jar io.github.collaboratory.LauncherCWL --config launcher.ini --descriptor collab.cwl --job collab-cwl-job-pre.json
    # if you want to skip the tests/checks on code quality
    # include -DskipTests -Dfindbugs.failOnError=false -Dcheckstyle.failOnViolation=false

## The Descriptor

In the case of this prototype the descriptors (`Collab.json` and `Collab.cwl`) are runtime descriptors.  So they contain fully resolvable entries.  We will need to think about how a general, non-runtime descriptor works that would be used during the registration process in the Dockstore.  The simplest approach might be to simply have them both be the same format but the one submitted with a registration request is considered the default values to use if a runtime descriptor is missing anything.

## The Workflow

It's a standard SeqWare workflow, designed to be built and run inside the Whitestar SeqWare Docker container.  To build it locally, you just build the Docker image:

    docker build -t collaboratory/workflow-helloworld:1.0.0 .

The locations on DockerHub and Quay.io:

* [docker pull quay.io/collaboratory/workflow-helloworld:1.0.0](https://quay.io/repository/collaboratory/workflow-helloworld)
* [docker pull collaboratory/workflow-helloworld:1.0.0](https://hub.docker.com/r/collaboratory/workflow-helloworld/)

## Other Examples in CWL

Here are some example Docker-based workflows in CWL (with Dockerfiles) from 7Bridges

* https://github.com/ntijanic/gatk-cocleaning-tool
* https://github.com/ntijanic/muse-tool

## TODO

* services needs to be fleshed out
* tests need to be fleshed out
* there is no real error checking, for production system will need to handle errors correctly/robustly.
