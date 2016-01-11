package com.cedarsoftware.ncube

import groovy.transform.CompileStatic
import org.junit.Test

/**
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br/>
 *         Copyright (c) Cedar Software LLC
 *         <br/><br/>
 *         Licensed under the Apache License, Version 2.0 (the "License")
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br/><br/>
 *         http://www.apache.org/licenses/LICENSE-2.0
 *         <br/><br/>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
@CompileStatic
class TestDelta
{
    @Test
    void testDeltaApis()
    {
        Delta x = new Delta(Delta.Location.AXIS, Delta.Type.ADD, "foo")
        assert x.location == Delta.Location.AXIS
        assert x.type == Delta.Type.ADD
        assert x.description == 'foo'
        assert x.toString() == 'foo'
    }

    @Test
    void testBadInputToChangeSetComparator()
    {
        assert !NCube.areDeltaSetsCompatible(null, [:])
        assert !NCube.areDeltaSetsCompatible([:], null)
    }

    @Test
    void testDiscreteMergeRemoveCol()
    {
        // Delete a column from cube 1, which not only deletes the column, but also the cells pointing to it.
        NCube cube1 = getTestCube()
        NCube cube2 = getTestCube()

        // Verify deletion occurred
        int count = cube1.cellMap.size()
        assert count == 48
        cube1.deleteColumn('state', 'GA')
        assert cube1.cellMap.size() == 32
        assert cube2.cellMap.size() == 48

        // Compute delta between copy of original cube and the cube with deleted column.
        // Apply this delta to the 2nd cube to force the same changes on it.
        Map<String, Object> delta1 = cube2.getDelta(cube1)
        Map<String, Object> delta2 = cube2.getDelta(cube2)  // Other guy made no changes

        boolean compatibleChange = NCube.areDeltaSetsCompatible(delta1, delta2)
        assert compatibleChange
        cube2.mergeDeltaSet(delta1)
        assert cube2.cellMap.size() == 32

        count = cube2.getAxis("state").getColumns().size()
        assert cube2.getAxis('state').findColumn('OH') != null
        assert cube2.getAxis('state').findColumn('TX') != null
        assert cube2.getAxis('state').findColumn('GA') == null
        assert count == 2
    }

    NCube getTestCube()
    {
        return NCube.fromSimpleJson(getTestCubeJson())
    }

    String getTestCubeJson()
    {
        return '''{
  "ncube": "testMerge",
  "axes": [
    {
      "name": "Age",
      "type": "RANGE",
      "valueType": "LONG",
      "preferredOrder": 0,
      "hasDefault": false,
      "fireAll": true,
      "columns": [
        {
          "id": 1000000000001,
          "value": [
            16,
            18
          ]
        },
        {
          "id": 1000000000002,
          "value": [
            18,
            22
          ]
        }
      ]
    },
    {
      "name": "Salary",
      "type": "SET",
      "valueType": "LONG",
      "preferredOrder": 0,
      "hasDefault": false,
      "fireAll": true,
      "columns": [
        {
          "id": 2000000000001,
          "value": [
            [
              60000,
              75000
            ]
          ]
        },
        {
          "id": 2000000000002,
          "value": [
            [
              75000,
              100000
            ]
          ]
        }
      ]
    },
    {
      "name": "Log",
      "type": "NEAREST",
      "valueType": "LONG",
      "preferredOrder": 0,
      "hasDefault": false,
      "fireAll": true,
      "columns": [
        {
          "id": 3000000000001,
          "type": "long",
          "value": 100
        },
        {
          "id": 3000000000002,
          "type": "long",
          "value": 1000
        }
      ]
    },
    {
      "name": "rule",
      "type": "RULE",
      "valueType": "EXPRESSION",
      "preferredOrder": 1,
      "hasDefault": false,
      "fireAll": true,
      "columns": [
        {
          "id": 4000000000001,
          "type": "exp",
          "name": "init",
          "value": "true"
        },
        {
          "id": 4000000000002,
          "type": "exp",
          "name": "process",
          "value": "true"
        }
      ]
    },
    {
      "name": "State",
      "type": "DISCRETE",
      "valueType": "STRING",
      "preferredOrder": 1,
      "hasDefault": false,
      "fireAll": true,
      "columns": [
        {
          "id": 5000000000002,
          "type": "string",
          "value": "GA"
        },
        {
          "id": 5000000000001,
          "type": "string",
          "value": "OH"
        },
        {
          "id": 5000000000003,
          "type": "string",
          "value": "TX"
        }
      ]
    }
  ],
  "cells": [
    {
      "id": [
        1000000000001,
        2000000000001,
        3000000000001,
        4000000000001,
        5000000000001
      ],
      "type": "string",
      "value": "1"
    },
    {
      "id": [
        1000000000001,
        2000000000001,
        3000000000001,
        4000000000001,
        5000000000002
      ],
      "type": "string",
      "value": "2"
    },
    {
      "id": [
        1000000000001,
        2000000000001,
        3000000000001,
        4000000000001,
        5000000000003
      ],
      "type": "string",
      "value": "3"
    },
    {
      "id": [
        1000000000001,
        2000000000001,
        3000000000001,
        4000000000002,
        5000000000001
      ],
      "type": "string",
      "value": "4"
    },
    {
      "id": [
        1000000000001,
        2000000000001,
        3000000000001,
        4000000000002,
        5000000000002
      ],
      "type": "string",
      "value": "5"
    },
    {
      "id": [
        1000000000001,
        2000000000001,
        3000000000001,
        4000000000002,
        5000000000003
      ],
      "type": "string",
      "value": "6"
    },
    {
      "id": [
        1000000000001,
        2000000000001,
        3000000000002,
        4000000000001,
        5000000000001
      ],
      "type": "string",
      "value": "7"
    },
    {
      "id": [
        1000000000001,
        2000000000001,
        3000000000002,
        4000000000001,
        5000000000002
      ],
      "type": "string",
      "value": "8"
    },
    {
      "id": [
        1000000000001,
        2000000000001,
        3000000000002,
        4000000000001,
        5000000000003
      ],
      "type": "string",
      "value": "9"
    },
    {
      "id": [
        1000000000001,
        2000000000001,
        3000000000002,
        4000000000002,
        5000000000001
      ],
      "type": "string",
      "value": "10"
    },
    {
      "id": [
        1000000000001,
        2000000000001,
        3000000000002,
        4000000000002,
        5000000000002
      ],
      "type": "string",
      "value": "11"
    },
    {
      "id": [
        1000000000001,
        2000000000001,
        3000000000002,
        4000000000002,
        5000000000003
      ],
      "type": "string",
      "value": "12"
    },
    {
      "id": [
        1000000000001,
        2000000000002,
        3000000000001,
        4000000000001,
        5000000000001
      ],
      "type": "string",
      "value": "13"
    },
    {
      "id": [
        1000000000001,
        2000000000002,
        3000000000001,
        4000000000001,
        5000000000002
      ],
      "type": "string",
      "value": "14"
    },
    {
      "id": [
        1000000000001,
        2000000000002,
        3000000000001,
        4000000000001,
        5000000000003
      ],
      "type": "string",
      "value": "15"
    },
    {
      "id": [
        1000000000001,
        2000000000002,
        3000000000001,
        4000000000002,
        5000000000001
      ],
      "type": "string",
      "value": "16"
    },
    {
      "id": [
        1000000000001,
        2000000000002,
        3000000000001,
        4000000000002,
        5000000000002
      ],
      "type": "string",
      "value": "17"
    },
    {
      "id": [
        1000000000001,
        2000000000002,
        3000000000001,
        4000000000002,
        5000000000003
      ],
      "type": "string",
      "value": "18"
    },
    {
      "id": [
        1000000000001,
        2000000000002,
        3000000000002,
        4000000000001,
        5000000000001
      ],
      "type": "string",
      "value": "19"
    },
    {
      "id": [
        1000000000001,
        2000000000002,
        3000000000002,
        4000000000001,
        5000000000002
      ],
      "type": "string",
      "value": "20"
    },
    {
      "id": [
        1000000000001,
        2000000000002,
        3000000000002,
        4000000000001,
        5000000000003
      ],
      "type": "string",
      "value": "21"
    },
    {
      "id": [
        1000000000001,
        2000000000002,
        3000000000002,
        4000000000002,
        5000000000001
      ],
      "type": "string",
      "value": "22"
    },
    {
      "id": [
        1000000000001,
        2000000000002,
        3000000000002,
        4000000000002,
        5000000000002
      ],
      "type": "string",
      "value": "23"
    },
    {
      "id": [
        1000000000001,
        2000000000002,
        3000000000002,
        4000000000002,
        5000000000003
      ],
      "type": "string",
      "value": "24"
    },
    {
      "id": [
        1000000000002,
        2000000000001,
        3000000000001,
        4000000000001,
        5000000000001
      ],
      "type": "string",
      "value": "25"
    },
    {
      "id": [
        1000000000002,
        2000000000001,
        3000000000001,
        4000000000001,
        5000000000002
      ],
      "type": "string",
      "value": "26"
    },
    {
      "id": [
        1000000000002,
        2000000000001,
        3000000000001,
        4000000000001,
        5000000000003
      ],
      "type": "string",
      "value": "27"
    },
    {
      "id": [
        1000000000002,
        2000000000001,
        3000000000001,
        4000000000002,
        5000000000001
      ],
      "type": "string",
      "value": "28"
    },
    {
      "id": [
        1000000000002,
        2000000000001,
        3000000000001,
        4000000000002,
        5000000000002
      ],
      "type": "string",
      "value": "29"
    },
    {
      "id": [
        1000000000002,
        2000000000001,
        3000000000001,
        4000000000002,
        5000000000003
      ],
      "type": "string",
      "value": "30"
    },
    {
      "id": [
        1000000000002,
        2000000000001,
        3000000000002,
        4000000000001,
        5000000000001
      ],
      "type": "string",
      "value": "31"
    },
    {
      "id": [
        1000000000002,
        2000000000001,
        3000000000002,
        4000000000001,
        5000000000002
      ],
      "type": "string",
      "value": "32"
    },
    {
      "id": [
        1000000000002,
        2000000000001,
        3000000000002,
        4000000000001,
        5000000000003
      ],
      "type": "string",
      "value": "33"
    },
    {
      "id": [
        1000000000002,
        2000000000001,
        3000000000002,
        4000000000002,
        5000000000001
      ],
      "type": "string",
      "value": "34"
    },
    {
      "id": [
        1000000000002,
        2000000000001,
        3000000000002,
        4000000000002,
        5000000000002
      ],
      "type": "string",
      "value": "35"
    },
    {
      "id": [
        1000000000002,
        2000000000001,
        3000000000002,
        4000000000002,
        5000000000003
      ],
      "type": "string",
      "value": "36"
    },
    {
      "id": [
        1000000000002,
        2000000000002,
        3000000000001,
        4000000000001,
        5000000000001
      ],
      "type": "string",
      "value": "37"
    },
    {
      "id": [
        1000000000002,
        2000000000002,
        3000000000001,
        4000000000001,
        5000000000002
      ],
      "type": "string",
      "value": "38"
    },
    {
      "id": [
        1000000000002,
        2000000000002,
        3000000000001,
        4000000000001,
        5000000000003
      ],
      "type": "string",
      "value": "39"
    },
    {
      "id": [
        1000000000002,
        2000000000002,
        3000000000001,
        4000000000002,
        5000000000001
      ],
      "type": "string",
      "value": "40"
    },
    {
      "id": [
        1000000000002,
        2000000000002,
        3000000000001,
        4000000000002,
        5000000000002
      ],
      "type": "string",
      "value": "41"
    },
    {
      "id": [
        1000000000002,
        2000000000002,
        3000000000001,
        4000000000002,
        5000000000003
      ],
      "type": "string",
      "value": "42"
    },
    {
      "id": [
        1000000000002,
        2000000000002,
        3000000000002,
        4000000000001,
        5000000000001
      ],
      "type": "string",
      "value": "43"
    },
    {
      "id": [
        1000000000002,
        2000000000002,
        3000000000002,
        4000000000001,
        5000000000002
      ],
      "type": "string",
      "value": "44"
    },
    {
      "id": [
        1000000000002,
        2000000000002,
        3000000000002,
        4000000000001,
        5000000000003
      ],
      "type": "string",
      "value": "45"
    },
    {
      "id": [
        1000000000002,
        2000000000002,
        3000000000002,
        4000000000002,
        5000000000001
      ],
      "type": "string",
      "value": "46"
    },
    {
      "id": [
        1000000000002,
        2000000000002,
        3000000000002,
        4000000000002,
        5000000000002
      ],
      "type": "string",
      "value": "47"
    },
    {
      "id": [
        1000000000002,
        2000000000002,
        3000000000002,
        4000000000002,
        5000000000003
      ],
      "type": "string",
      "value": "48"
    }
  ]
}'''

    }
}
