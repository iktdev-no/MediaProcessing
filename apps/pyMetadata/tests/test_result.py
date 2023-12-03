import unittest
import json
from sources.result import Metadata, DataResult

class SerializationTest(unittest.TestCase):
    def test_metadata_to_json(self):
        metadata = Metadata(
            title='Sample Title',
            altTitle='Alternate Title',
            cover='path/to/cover.jpg',
            type='Movie',
            summary='Lorem ipsum dolor sit amet',
            genres=['Action', 'Drama', 'Thriller']
        )

        metadata_json = json.dumps(metadata.to_dict())
        self.assertIsInstance(metadata_json, str)

    def test_data_result_to_json(self):
        metadata = Metadata(
            title='Sample Title',
            altTitle='Alternate Title',
            cover='path/to/cover.jpg',
            type='Movie',
            summary='Lorem ipsum dolor sit amet',
            genres=['Action', 'Drama', 'Thriller']
        )

        data_result = DataResult(
            statusType='SUCCESS',
            errorMessage=None,
            data=metadata
        )

        data_result_json = json.dumps(data_result.to_dict())
        self.assertIsInstance(data_result_json, str)

if __name__ == '__main__':
    unittest.main()
