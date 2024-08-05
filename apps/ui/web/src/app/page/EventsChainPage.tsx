import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useStompClient } from 'react-stomp-hooks';
import { RootState } from "../store";
import { useWsSubscription } from "../ws/subscriptions";
import { set } from "../store/persistent-events-slice";
import { EventsObjectListResponse } from "../../types";
import IconRefresh from '@mui/icons-material/Refresh'
import { Button } from "@mui/material";


export default function EventsChainPage() {
    const dispatch = useDispatch();
    const client = useStompClient();
    const cursor = useSelector((state: RootState) => state.persistentEvents)

    function log(data: any) {
        console.log(data)
    }

    useWsSubscription("/topic/chained/all", (response) => {
        console.log(response)
    });

    useEffect(() => {
    

        // Kjør din funksjon her når komponenten lastes inn for første gang
        // Sjekk om cursor er null
        if (cursor.items === null && client !== null) {
          console.log(cursor)
          // Kjør din funksjon her når cursor er null og client ikke er null
          client?.publish({
            destination: "/app/chained/all"
          });
    
          // Alternativt, du kan dispatche en Redux handling her
          // dispatch(fetchDataAction()); // Eksempel på å dispatche en handling
        }
      }, [cursor, client, dispatch]);

      const onRefresh = () => {
        client?.publish({
            "destination": "/app/chained/all",
            "body": "Potato"
        })
    }
    return (
        <>
            <Button
                startIcon={ <IconRefresh /> }
                onClick={onRefresh} sx={{
                borderRadius: 5,
                textTransform: 'none'
            }}>Refresh</Button >
        </>
    )
}