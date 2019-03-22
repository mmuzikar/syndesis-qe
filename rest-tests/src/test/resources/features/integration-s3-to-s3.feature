# @sustainer: tplevko@redhat.com

@rest
@s3
Feature: Integration - S3

  Background:
    Given clean application state
      And create sample buckets on S3 with name "syndesis-server-bucket-in"
      And create sample buckets on S3 with name "syndesis-server-bucket-out"
      And check that buckets do exist: "syndesis-server-bucket-in, syndesis-server-bucket-out"

  @integration-s3-s3-create
  Scenario: S3 to S3 - Create
    Given create S3 connection using "syndesis-server-bucket-out" bucket
      And create S3 connection using "syndesis-server-bucket-in" bucket
    When create S3 polling START action step with bucket: "syndesis-server-bucket-out"
      And create S3 copy FINISH action step with bucket: "syndesis-server-bucket-in"
      And create integration with name: "S3 to S3 rest test"
      And wait for integration with name: "S3 to S3 rest test" to become active
      And create a new text file in bucket "syndesis-server-bucket-out" with name "test.txt" and text "Hello world!"
    Then validate bucket with name "syndesis-server-bucket-in" contains file with name "test.txt" and text "Hello world!"

  @integration-s3-s3-delete-all
  Scenario: S3 to S3 - Delete All
    Given create S3 connection using "syndesis-server-bucket-out" bucket
      And create a new text file in bucket "syndesis-server-bucket-out" with name "testdelete1.txt" and text "Hello world!"
      And create a new text file in bucket "syndesis-server-bucket-out" with name "testdelete2.txt" and text "Hello world!"
      And validate bucket with name "syndesis-server-bucket-out" contains file with name "testdelete1.txt" and text "Hello world!"
      And validate bucket with name "syndesis-server-bucket-out" contains file with name "testdelete2.txt" and text "Hello world!"
    When create S3 polling START action step with bucket: "syndesis-server-bucket-out"
      And create S3 delete FINISH action step with bucket: "syndesis-server-bucket-out"
      And create integration with name: "delete all from S3 rest test"
      And wait for integration with name: "delete all from S3 rest test" to become active
    Then validate bucket with name "syndesis-server-bucket-out" does not contain file with name "testdelete1.txt"
      And validate bucket with name "syndesis-server-bucket-out" does not contain file with name "testdelete2.txt"

  @integration-s3-s3-delete-all-prefixed
  Scenario: S3 to S3 - Delete All Prefixed
    Given create S3 connection using "syndesis-server-bucket-out" bucket
      And create a new text file in bucket "syndesis-server-bucket-out" with name "testdelete1.txt" and text "Hello world!"
      And create a new text file in bucket "syndesis-server-bucket-out" with name "testdelete2.txt" and text "Hello world!"
      And validate bucket with name "syndesis-server-bucket-out" contains file with name "testdelete1.txt" and text "Hello world!"
      And validate bucket with name "syndesis-server-bucket-out" contains file with name "testdelete2.txt" and text "Hello world!"
    When create S3 polling START action step with bucket: "syndesis-server-bucket-out" and prefix "testdelete1"
      And create S3 delete FINISH action step with bucket: "syndesis-server-bucket-out"
      And create integration with name: "delete all prefixed from S3 rest test"
      And wait for integration with name: "delete all prefixed from S3 rest test" to become active
    Then validate bucket with name "syndesis-server-bucket-out" does not contain file with name "testdelete1.txt"
      And validate bucket with name "syndesis-server-bucket-out" contains file with name "testdelete2.txt" and text "Hello world!"

  @integration-s3-s3-delete-filtered
  Scenario: S3 to S3 - Delete Filtered
    Given create S3 connection using "syndesis-server-bucket-out" bucket
      And create a new text file in bucket "syndesis-server-bucket-out" with name "testdelete1.txt" and text "Hello world!"
      And create a new text file in bucket "syndesis-server-bucket-out" with name "testdelete2.txt" and text "Hello world!"
      And validate bucket with name "syndesis-server-bucket-out" contains file with name "testdelete1.txt" and text "Hello world!"
      And validate bucket with name "syndesis-server-bucket-out" contains file with name "testdelete2.txt" and text "Hello world!"
    When create S3 polling START action step with bucket: "syndesis-server-bucket-out"
      And create S3 delete FINISH action step with bucket: "syndesis-server-bucket-out" and filename: "testdelete1.txt"
      And create integration with name: "delete filtered from S3 rest test"
      And wait for integration with name: "delete filtered from S3 rest test" to become active
    Then validate bucket with name "syndesis-server-bucket-out" does not contain file with name "testdelete1.txt"
      And validate bucket with name "syndesis-server-bucket-out" contains file with name "testdelete2.txt" and text "Hello world!"
