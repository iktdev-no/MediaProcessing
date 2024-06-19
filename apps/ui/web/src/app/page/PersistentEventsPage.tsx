import { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { useStompClient } from 'react-stomp-hooks';
import { RootState } from "../store";
import { useWsSubscription } from "../ws/subscriptions";
import { set } from "../store/persistent-events-slice";
import { EventsObjectListResponse } from "../../types";

export default function PersistentEventsPage() {
    const dispatch = useDispatch();
    const client = useStompClient();
    const cursor = useSelector((state: RootState) => state.persistentEvents)

    useWsSubscription<EventsObjectListResponse>("/topic/persistent/events", (response) => {
        dispatch(set(response))
    });

    useEffect(() => {
    

        // Kjør din funksjon her når komponenten lastes inn for første gang
        // Sjekk om cursor er null
        if (cursor.items === null && client !== null) {
          console.log(cursor)
          // Kjør din funksjon her når cursor er null og client ikke er null
          client?.publish({
            destination: "/app/persistent/events"
          });
    
          // Alternativt, du kan dispatche en Redux handling her
          // dispatch(fetchDataAction()); // Eksempel på å dispatche en handling
        }
      }, [cursor, client, dispatch]);


    return (
        <></>
    )
}